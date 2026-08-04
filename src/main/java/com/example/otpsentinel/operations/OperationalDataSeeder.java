package com.example.otpsentinel.operations;

import com.example.otpsentinel.operations.OperationalDataGenerator.DeliverySample;
import com.example.otpsentinel.operations.OperationalDataGenerator.ErrorSample;
import com.example.otpsentinel.operations.OperationalDataGenerator.MinuteSlice;
import com.example.otpsentinel.operations.OperationalDataGenerator.ProviderHealthSample;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Fills the operational tables so every time window an operator can ask about actually has rows.
 * Backfills the configured history on startup and appends one minute at a time while the app runs.
 *
 * <p>The incident is anchored to startup: minutes from {@code startup - degradedLookback} onwards
 * are degraded, so "the last 15 minutes" always shows the anomaly and the rows behind it stay
 * inspectable. Every write is {@code ON CONFLICT DO NOTHING}, so a restart re-seeds nothing.
 */
public class OperationalDataSeeder implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(OperationalDataSeeder.class);

  private final JdbcTemplate jdbcTemplate;
  private final Clock clock;
  private final Duration history;
  private final Duration degradedLookback;
  private Instant degradationStart;

  public OperationalDataSeeder(
      JdbcTemplate jdbcTemplate, Clock clock, Duration history, Duration degradedLookback) {
    this.jdbcTemplate = jdbcTemplate;
    this.clock = clock;
    this.history = history;
    this.degradedLookback = degradedLookback;
  }

  @Override
  public void run(ApplicationArguments args) {
    seedHistory();
  }

  public void seedHistory() {
    Instant now = clock.instant().truncatedTo(ChronoUnit.MINUTES);
    degradationStart = now.minus(degradedLookback);
    Instant from = now.minus(history);
    List<Instant> minutes = new ArrayList<>();
    for (Instant minute = from; !minute.isAfter(now); minute = minute.plusSeconds(60)) {
      minutes.add(minute);
    }
    writeMinutes(minutes, minute -> !minute.isBefore(degradationStart));
    seedChangeEvents(degradationStart);
    LOG.info(
        "operational data seeded: {} minutes, degradation from {}", minutes.size(), degradationStart);
  }

  /** Keeps "now" populated while the console is open; the incident stays ongoing. */
  @Scheduled(fixedRate = 60_000L)
  public void appendCurrentMinute() {
    if (degradationStart == null) {
      return;
    }
    Instant minute = clock.instant().truncatedTo(ChronoUnit.MINUTES);
    writeMinute(minute, !minute.isBefore(degradationStart));
  }

  private void writeMinute(Instant bucketAt, boolean degraded) {
    writeMinutes(List.of(bucketAt), minute -> degraded);
  }

  /** Batched: a 24 h backfill is ~1440 minutes × 4 providers, far too many single round trips. */
  private void writeMinutes(List<Instant> minutes, java.util.function.Predicate<Instant> degraded) {
    List<MinuteSlice> slices =
        minutes.stream().map(minute -> OperationalDataGenerator.minute(minute, degraded.test(minute))).toList();

    jdbcTemplate.batchUpdate(
        """
        INSERT INTO otp_delivery_sample
          (bucket_at, provider, attempted, delivered, failed, avg_delivery_seconds,
           p95_delivery_seconds, retries)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (bucket_at, provider) DO NOTHING
        """,
        slices.stream()
            .flatMap(slice -> slice.deliveries().stream())
            .map(
                (DeliverySample sample) ->
                    new Object[] {
                      Timestamp.from(sample.bucketAt()),
                      sample.provider(),
                      sample.attempted(),
                      sample.delivered(),
                      sample.failed(),
                      sample.averageDeliverySeconds(),
                      sample.p95DeliverySeconds(),
                      sample.retries()
                    })
            .toList());

    jdbcTemplate.batchUpdate(
        """
        INSERT INTO otp_error_sample (bucket_at, provider, error_code, failures)
        VALUES (?, ?, ?, ?)
        ON CONFLICT (bucket_at, provider, error_code) DO NOTHING
        """,
        slices.stream()
            .flatMap(slice -> slice.errors().stream())
            .map(
                (ErrorSample sample) ->
                    new Object[] {
                      Timestamp.from(sample.bucketAt()),
                      sample.provider(),
                      sample.errorCode(),
                      sample.failures()
                    })
            .toList());

    jdbcTemplate.batchUpdate(
        """
        INSERT INTO provider_health_sample
          (bucket_at, provider, status, avg_response_seconds, timeout_rate,
           circuit_breaker_state, active_connections, max_connections, last_successful_request_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (bucket_at, provider) DO NOTHING
        """,
        slices.stream()
            .flatMap(slice -> slice.providerHealth().stream())
            .map(
                (ProviderHealthSample sample) ->
                    new Object[] {
                      Timestamp.from(sample.bucketAt()),
                      sample.provider(),
                      sample.status(),
                      sample.averageResponseSeconds(),
                      sample.timeoutRate(),
                      sample.circuitBreakerState(),
                      sample.activeConnections(),
                      sample.maxConnections(),
                      Timestamp.from(sample.lastSuccessfulRequestAt())
                    })
            .toList());

    jdbcTemplate.batchUpdate(
        """
        INSERT INTO queue_health_sample
          (bucket_at, pending_messages, normal_pending_threshold, oldest_message_age_seconds,
           normal_oldest_age_threshold_seconds, active_consumers, expected_consumers,
           dead_letter_count, processing_rate_status, status)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (bucket_at) DO NOTHING
        """,
        slices.stream()
            .map(
                slice ->
                    new Object[] {
                      Timestamp.from(slice.queue().bucketAt()),
                      slice.queue().pendingMessages(),
                      slice.queue().normalPendingThreshold(),
                      slice.queue().oldestMessageAgeSeconds(),
                      slice.queue().normalOldestAgeThresholdSeconds(),
                      slice.queue().activeConsumers(),
                      slice.queue().expectedConsumers(),
                      slice.queue().deadLetterCount(),
                      slice.queue().processingRateStatus(),
                      slice.queue().status()
                    })
            .toList());
  }

  private void seedChangeEvents(Instant degradationStart) {
    insertChange(
        "chg-101",
        degradationStart.minus(Duration.ofMinutes(10)),
        "CONFIG",
        "OTP_GATEWAY",
        "Retry count changed from 3 to 2",
        null,
        true);
    insertChange(
        "chg-102",
        degradationStart.minus(Duration.ofMinutes(3)),
        "DEPLOY",
        "OTP_GATEWAY",
        "Gateway v2.4 deployed",
        "v2.4",
        true);
    insertChange(
        "obs-103",
        degradationStart.plus(Duration.ofMinutes(2)),
        "OBSERVATION",
        "PROVIDER_ADAPTER",
        "OPERATOR_B adapter response time increasing",
        null,
        null);
  }

  private void insertChange(
      String changeId,
      Instant occurredAt,
      String type,
      String component,
      String description,
      String version,
      Boolean approved) {
    jdbcTemplate.update(
        """
        INSERT INTO change_event
          (change_id, occurred_at, type, component, description, version, approved)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (change_id) DO NOTHING
        """,
        changeId,
        Timestamp.from(occurredAt),
        type,
        component,
        description,
        version,
        approved);
  }
}
