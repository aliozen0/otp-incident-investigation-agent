package com.example.otpsentinel.api;

import com.example.otpsentinel.api.dto.OperationsDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only window into the same operational rows the agent's tools query, so an operator can put
 * the model's answer next to the raw data instead of trusting it. Nothing here is model-generated.
 */
@RestController
@RequestMapping("/api/v1/operations")
@Tag(name = "Operations", description = "Raw operational telemetry behind every investigation")
public class OperationsController {

  private static final Duration MAX_WINDOW = Duration.ofHours(24);
  private static final Duration DEFAULT_WINDOW = Duration.ofMinutes(60);
  private static final int MAX_SAMPLE_ROWS = 2000;

  private final JdbcTemplate jdbcTemplate;

  public OperationsController(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @GetMapping("/overview")
  public OperationsDto.Overview overview(
      @RequestParam(required = false) String startAt, @RequestParam(required = false) String endAt) {
    Instant end = parse(endAt, Instant.now()).truncatedTo(ChronoUnit.MINUTES).plusSeconds(60);
    Instant start = parse(startAt, end.minus(DEFAULT_WINDOW)).truncatedTo(ChronoUnit.MINUTES);
    validate(start, end);

    List<OperationsDto.SeriesPoint> series =
        jdbcTemplate.query(
            """
            SELECT bucket_at,
                   SUM(attempted) AS attempted,
                   SUM(delivered) AS delivered,
                   SUM(failed) AS failed,
                   SUM(retries) AS retries,
                   SUM(avg_delivery_seconds * attempted) / NULLIF(SUM(attempted), 0) AS avg_seconds,
                   MAX(p95_delivery_seconds) AS p95_seconds
            FROM otp_delivery_sample
            WHERE bucket_at >= ? AND bucket_at < ?
            GROUP BY bucket_at
            ORDER BY bucket_at
            """,
            (rs, rowNum) -> {
              long attempted = rs.getLong("attempted");
              long delivered = rs.getLong("delivered");
              return new OperationsDto.SeriesPoint(
                  rs.getTimestamp("bucket_at").toInstant(),
                  attempted,
                  delivered,
                  rs.getLong("failed"),
                  rs.getLong("retries"),
                  attempted == 0 ? 0.0 : round2(delivered * 100.0 / attempted),
                  round2(rs.getDouble("avg_seconds")),
                  round2(rs.getDouble("p95_seconds")));
            },
            Timestamp.from(start),
            Timestamp.from(end));

    long attempted = series.stream().mapToLong(OperationsDto.SeriesPoint::attempted).sum();
    long delivered = series.stream().mapToLong(OperationsDto.SeriesPoint::delivered).sum();
    long failed = series.stream().mapToLong(OperationsDto.SeriesPoint::failed).sum();
    long retries = series.stream().mapToLong(OperationsDto.SeriesPoint::retries).sum();
    double p95 =
        series.stream().mapToDouble(OperationsDto.SeriesPoint::p95DeliverySeconds).max().orElse(0.0);
    double weightedSeconds =
        series.stream()
            .mapToDouble(point -> point.averageDeliverySeconds() * point.attempted())
            .sum();

    List<OperationsDto.ProviderRow> providers =
        jdbcTemplate.query(
            """
            -- Health is a state, not something to aggregate: take the newest sample in the window
            -- per provider (MAX(status) would sort 'HEALTHY' above 'DEGRADED' and hide the incident).
            SELECT totals.provider, totals.attempted, totals.delivered, totals.failed,
                   latest.status, latest.avg_response_seconds, latest.timeout_rate,
                   latest.circuit_breaker_state, latest.active_connections, latest.max_connections
            FROM (
              SELECT provider,
                     SUM(attempted) AS attempted,
                     SUM(delivered) AS delivered,
                     SUM(failed) AS failed
              FROM otp_delivery_sample
              WHERE bucket_at >= ? AND bucket_at < ?
              GROUP BY provider
            ) totals
            LEFT JOIN LATERAL (
              SELECT * FROM provider_health_sample h
              WHERE h.provider = totals.provider AND h.bucket_at >= ? AND h.bucket_at < ?
              ORDER BY h.bucket_at DESC LIMIT 1
            ) latest ON TRUE
            ORDER BY totals.provider
            """,
            (rs, rowNum) -> {
              long providerAttempted = rs.getLong("attempted");
              long providerDelivered = rs.getLong("delivered");
              return new OperationsDto.ProviderRow(
                  rs.getString("provider"),
                  providerAttempted,
                  providerDelivered,
                  rs.getLong("failed"),
                  providerAttempted == 0
                      ? 0.0
                      : round2(providerDelivered * 100.0 / providerAttempted),
                  rs.getString("status"),
                  round2(rs.getDouble("avg_response_seconds")),
                  rs.getDouble("timeout_rate"),
                  rs.getString("circuit_breaker_state"),
                  rs.getInt("active_connections"),
                  rs.getInt("max_connections"));
            },
            Timestamp.from(start),
            Timestamp.from(end),
            Timestamp.from(start),
            Timestamp.from(end));

    List<OperationsDto.ErrorRow> errors =
        jdbcTemplate.query(
            """
            SELECT error_code, SUM(failures) AS failures
            FROM otp_error_sample
            WHERE bucket_at >= ? AND bucket_at < ?
            GROUP BY error_code
            ORDER BY failures DESC
            """,
            (rs, rowNum) -> new OperationsDto.ErrorRow(rs.getString("error_code"), rs.getLong("failures"), 0.0),
            Timestamp.from(start),
            Timestamp.from(end));
    long errorTotal = errors.stream().mapToLong(OperationsDto.ErrorRow::failures).sum();
    List<OperationsDto.ErrorRow> errorsWithShare =
        errors.stream()
            .map(
                row ->
                    new OperationsDto.ErrorRow(
                        row.errorCode(),
                        row.failures(),
                        errorTotal == 0 ? 0.0 : round2(row.failures() * 100.0 / errorTotal)))
            .toList();

    List<OperationsDto.QueueRow> queue =
        jdbcTemplate.query(
            """
            SELECT * FROM queue_health_sample
            WHERE bucket_at >= ? AND bucket_at < ?
            ORDER BY bucket_at DESC LIMIT 1
            """,
            (rs, rowNum) ->
                new OperationsDto.QueueRow(
                    rs.getTimestamp("bucket_at").toInstant(),
                    rs.getLong("pending_messages"),
                    rs.getLong("normal_pending_threshold"),
                    rs.getLong("oldest_message_age_seconds"),
                    rs.getInt("active_consumers"),
                    rs.getInt("expected_consumers"),
                    rs.getLong("dead_letter_count"),
                    rs.getString("processing_rate_status"),
                    rs.getString("status")),
            Timestamp.from(start),
            Timestamp.from(end));

    List<OperationsDto.ChangeRow> changes =
        jdbcTemplate.query(
            """
            SELECT * FROM change_event
            WHERE occurred_at >= ? AND occurred_at < ?
            ORDER BY occurred_at
            """,
            (rs, rowNum) ->
                new OperationsDto.ChangeRow(
                    rs.getString("change_id"),
                    rs.getTimestamp("occurred_at").toInstant(),
                    rs.getString("type"),
                    rs.getString("component"),
                    rs.getString("description"),
                    rs.getString("version"),
                    rs.getObject("approved") == null ? null : rs.getBoolean("approved")),
            Timestamp.from(start),
            Timestamp.from(end));

    return new OperationsDto.Overview(
        start,
        end,
        new OperationsDto.Totals(
            attempted,
            delivered,
            failed,
            retries,
            attempted == 0 ? 0.0 : round2(delivered * 100.0 / attempted),
            attempted == 0 ? 0.0 : round2(weightedSeconds / attempted),
            round2(p95)),
        series,
        providers,
        errorsWithShare,
        queue.isEmpty() ? null : queue.getFirst(),
        changes);
  }

  /** Row-level view: exactly what the aggregates above are computed from. */
  @GetMapping("/samples")
  public List<OperationsDto.SampleRow> samples(
      @RequestParam(required = false) String startAt,
      @RequestParam(required = false) String endAt,
      @RequestParam(required = false) String provider) {
    Instant end = parse(endAt, Instant.now()).truncatedTo(ChronoUnit.MINUTES).plusSeconds(60);
    Instant start = parse(startAt, end.minus(DEFAULT_WINDOW)).truncatedTo(ChronoUnit.MINUTES);
    validate(start, end);
    String providerFilter = provider == null || provider.isBlank() ? null : provider;
    return jdbcTemplate.query(
        """
        SELECT d.bucket_at, d.provider, d.attempted, d.delivered, d.failed, d.retries,
               d.avg_delivery_seconds, d.p95_delivery_seconds, h.status, h.timeout_rate,
               (SELECT string_agg(e.error_code || '=' || e.failures, ', ' ORDER BY e.failures DESC)
                  FROM otp_error_sample e
                 WHERE e.bucket_at = d.bucket_at AND e.provider = d.provider) AS errors
        FROM otp_delivery_sample d
        LEFT JOIN provider_health_sample h
          ON h.provider = d.provider AND h.bucket_at = d.bucket_at
        WHERE d.bucket_at >= ? AND d.bucket_at < ?
          AND (?::text IS NULL OR d.provider = ?::text)
        ORDER BY d.bucket_at DESC, d.provider
        LIMIT ?
        """,
        (rs, rowNum) ->
            new OperationsDto.SampleRow(
                rs.getTimestamp("bucket_at").toInstant(),
                rs.getString("provider"),
                rs.getLong("attempted"),
                rs.getLong("delivered"),
                rs.getLong("failed"),
                rs.getLong("retries"),
                round2(rs.getDouble("avg_delivery_seconds")),
                round2(rs.getDouble("p95_delivery_seconds")),
                rs.getString("status"),
                rs.getDouble("timeout_rate"),
                rs.getString("errors")),
        Timestamp.from(start),
        Timestamp.from(end),
        providerFilter,
        providerFilter,
        MAX_SAMPLE_ROWS);
  }

  private static void validate(Instant start, Instant end) {
    if (!end.isAfter(start)) {
      throw new ApiException(
          400, "INVALID_TIME_WINDOW", "Invalid time window", "endAt must be after startAt");
    }
    if (Duration.between(start, end).compareTo(MAX_WINDOW) > 0) {
      throw new ApiException(
          400, "INVALID_TIME_WINDOW", "Invalid time window", "window must not exceed 24 hours");
    }
  }

  private static Instant parse(String value, Instant fallback) {
    if (value == null || value.isBlank()) {
      return fallback;
    }
    try {
      return Instant.parse(value);
    } catch (DateTimeParseException invalid) {
      throw new ApiException(
          400, "INVALID_TIME_WINDOW", "Invalid time window", "timestamps must be ISO-8601 instants");
    }
  }

  private static double round2(double value) {
    return Math.round(value * 100.0) / 100.0;
  }
}
