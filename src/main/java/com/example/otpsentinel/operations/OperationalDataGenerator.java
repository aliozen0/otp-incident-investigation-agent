package com.example.otpsentinel.operations;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minute-by-minute operational telemetry. Deterministic but not uniform: every figure comes from a
 * hash of (minute, provider), so the data varies the way production data does — each provider has
 * its own traffic share, failure baseline and latency, volume follows a daily curve, and no two
 * minutes look alike — while a rerun still reproduces the exact same database. An operator can
 * therefore recompute any aggregate the agent cites, and the numbers still look like real traffic.
 *
 * <p>Healthy minutes sit near 98% success with ~2 s delivery. During a degradation OPERATOR_B
 * collapses to roughly half its deliveries with double-digit latency, which drags the overall
 * window to the low 70s — the shape docs/15-demo-fixtures.md describes for OTP-DROP-001. The queue
 * stays healthy in both modes because the correct analysis here is "provider degradation, not
 * queue backlog".
 */
public final class OperationalDataGenerator {

  public record DeliverySample(
      Instant bucketAt,
      String provider,
      long attempted,
      long delivered,
      long failed,
      double averageDeliverySeconds,
      double p95DeliverySeconds,
      long retries) {}

  public record ErrorSample(Instant bucketAt, String provider, String errorCode, long failures) {}

  public record ProviderHealthSample(
      Instant bucketAt,
      String provider,
      String status,
      double averageResponseSeconds,
      double timeoutRate,
      String circuitBreakerState,
      int activeConnections,
      int maxConnections,
      Instant lastSuccessfulRequestAt) {}

  public record QueueSample(
      Instant bucketAt,
      long pendingMessages,
      long normalPendingThreshold,
      long oldestMessageAgeSeconds,
      long normalOldestAgeThresholdSeconds,
      int activeConsumers,
      int expectedConsumers,
      long deadLetterCount,
      String processingRateStatus,
      String status) {}

  public record MinuteSlice(
      List<DeliverySample> deliveries,
      List<ErrorSample> errors,
      List<ProviderHealthSample> providerHealth,
      QueueSample queue) {}

  /**
   * @param baseAttempts average attempts per minute at peak traffic
   * @param healthyFailureRate baseline failure ratio
   * @param healthyLatency average delivery seconds when healthy
   */
  private record ProviderProfile(
      String name,
      long baseAttempts,
      double healthyFailureRate,
      double healthyLatency,
      int maxConnections) {}

  private static final List<ProviderProfile> PROVIDERS =
      List.of(
          new ProviderProfile("OPERATOR_A", 372, 0.017, 1.9, 60),
          new ProviderProfile("OPERATOR_B", 486, 0.021, 2.3, 50),
          new ProviderProfile("OPERATOR_C", 214, 0.029, 2.7, 40),
          new ProviderProfile("OPERATOR_D", 128, 0.012, 1.6, 30));

  private static final String DEGRADED_PROVIDER = "OPERATOR_B";

  private static final Map<String, Double> DEGRADED_ERROR_SHARES =
      Map.of(
          "PROVIDER_TIMEOUT", 0.64,
          "RATE_LIMITED", 0.18,
          "CONNECTION_RESET", 0.11,
          "INVALID_NUMBER", 0.04,
          "UNKNOWN", 0.03);

  private static final Map<String, Double> HEALTHY_ERROR_SHARES =
      Map.of(
          "INVALID_NUMBER", 0.55,
          "UNKNOWN", 0.25,
          "CONNECTION_RESET", 0.12,
          "RATE_LIMITED", 0.05,
          "PROVIDER_TIMEOUT", 0.03);

  private OperationalDataGenerator() {}

  public static MinuteSlice minute(Instant bucketAt, boolean degraded) {
    List<DeliverySample> deliveries = new ArrayList<>();
    List<ErrorSample> errors = new ArrayList<>();
    List<ProviderHealthSample> health = new ArrayList<>();
    long epochMinute = bucketAt.getEpochSecond() / 60;
    double trafficCurve = dailyCurve(bucketAt);

    for (ProviderProfile provider : PROVIDERS) {
      boolean providerDegraded = degraded && provider.name().equals(DEGRADED_PROVIDER);
      double volumeJitter = 0.85 + 0.3 * noise(epochMinute, provider.name(), "volume");
      long attempted = Math.max(1, Math.round(provider.baseAttempts() * trafficCurve * volumeJitter));

      double failureRate =
          providerDegraded
              ? 0.44 + 0.10 * noise(epochMinute, provider.name(), "degraded")
              : provider.healthyFailureRate() * (0.7 + 0.6 * noise(epochMinute, provider.name(), "fail"));
      long failed = Math.min(attempted, Math.round(attempted * failureRate));
      long delivered = attempted - failed;

      double latency =
          providerDegraded
              ? 11.5 + 5.0 * noise(epochMinute, provider.name(), "latency")
              : provider.healthyLatency() * (0.8 + 0.45 * noise(epochMinute, provider.name(), "latency"));
      double p95 = latency * (1.9 + 0.6 * noise(epochMinute, provider.name(), "p95"));
      long retries = Math.round(failed * (0.6 + 0.8 * noise(epochMinute, provider.name(), "retry")));

      deliveries.add(
          new DeliverySample(
              bucketAt,
              provider.name(),
              attempted,
              delivered,
              failed,
              round2(latency),
              round2(p95),
              retries));

      Map<String, Long> byCode =
          allocate(failed, providerDegraded ? DEGRADED_ERROR_SHARES : HEALTHY_ERROR_SHARES);
      byCode.forEach(
          (code, count) -> {
            if (count > 0) {
              errors.add(new ErrorSample(bucketAt, provider.name(), code, count));
            }
          });

      double timeoutRate =
          providerDegraded
              ? round2(0.26 + 0.10 * noise(epochMinute, provider.name(), "timeout"))
              : round3(0.005 + 0.02 * noise(epochMinute, provider.name(), "timeout"));
      int activeConnections =
          providerDegraded
              ? (int) Math.round(provider.maxConnections() * (0.9 + 0.09 * noise(epochMinute, provider.name(), "conn")))
              : (int) Math.round(provider.maxConnections() * (0.18 + 0.22 * noise(epochMinute, provider.name(), "conn")));

      health.add(
          new ProviderHealthSample(
              bucketAt,
              provider.name(),
              providerDegraded ? "DEGRADED" : "HEALTHY",
              round2(latency),
              timeoutRate,
              providerDegraded ? "HALF_OPEN" : "CLOSED",
              Math.max(1, activeConnections),
              provider.maxConnections(),
              providerDegraded ? bucketAt.minusSeconds(18 + (long) (40 * noise(epochMinute, provider.name(), "last"))) : bucketAt));
    }

    long backlog =
        Math.round((degraded ? 150 : 70) * (0.7 + 0.7 * noise(epochMinute, "QUEUE", "pending")));
    QueueSample queue =
        new QueueSample(
            bucketAt,
            backlog,
            1_000,
            degraded ? 3 + Math.round(4 * noise(epochMinute, "QUEUE", "age")) : 1 + Math.round(2 * noise(epochMinute, "QUEUE", "age")),
            30,
            8,
            8,
            degraded ? 2 + Math.round(3 * noise(epochMinute, "QUEUE", "dlq")) : Math.round(2 * noise(epochMinute, "QUEUE", "dlq")),
            "NORMAL",
            "HEALTHY");

    return new MinuteSlice(List.copyOf(deliveries), List.copyOf(errors), List.copyOf(health), queue);
  }

  /** Night traffic is roughly half of the afternoon peak, smoothly. */
  private static double dailyCurve(Instant bucketAt) {
    int hour = bucketAt.atZone(ZoneOffset.UTC).getHour();
    double phase = (hour - 4) / 24.0 * 2 * Math.PI;
    return 0.75 + 0.35 * Math.sin(phase);
  }

  /** Stable hash noise in [0,1): same inputs always produce the same value. */
  private static double noise(long minute, String provider, String channel) {
    long seed = minute * 0x9E3779B97F4A7C15L + provider.hashCode() * 0xBF58476D1CE4E5B9L;
    seed ^= channel.hashCode() * 0x94D049BB133111EBL;
    seed ^= seed >>> 30;
    seed *= 0xBF58476D1CE4E5B9L;
    seed ^= seed >>> 27;
    seed *= 0x94D049BB133111EBL;
    seed ^= seed >>> 31;
    return (seed >>> 11) / (double) (1L << 53);
  }

  /** Largest-remainder split so the per-code counts always add back up to {@code total}. */
  private static Map<String, Long> allocate(long total, Map<String, Double> shares) {
    Map<String, Long> counts = new LinkedHashMap<>();
    if (total <= 0) {
      return counts;
    }
    List<String> codes = shares.keySet().stream().sorted().toList();
    long assigned = 0;
    String largest = codes.getFirst();
    double largestShare = -1;
    for (String code : codes) {
      double share = shares.get(code);
      long count = (long) Math.floor(total * share);
      counts.put(code, count);
      assigned += count;
      if (share > largestShare) {
        largestShare = share;
        largest = code;
      }
    }
    counts.merge(largest, total - assigned, Long::sum);
    return counts;
  }

  private static double round2(double value) {
    return Math.round(value * 100.0) / 100.0;
  }

  private static double round3(double value) {
    return Math.round(value * 1000.0) / 1000.0;
  }
}
