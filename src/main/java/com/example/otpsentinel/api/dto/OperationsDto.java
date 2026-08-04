package com.example.otpsentinel.api.dto;

import java.time.Instant;
import java.util.List;

/** Wire shapes for the operational data explorer. Raw rows and their aggregates, nothing derived. */
public final class OperationsDto {

  private OperationsDto() {}

  public record Overview(
      Instant startAt,
      Instant endAt,
      Totals totals,
      List<SeriesPoint> series,
      List<ProviderRow> providers,
      List<ErrorRow> errors,
      QueueRow queue,
      List<ChangeRow> changes) {}

  public record Totals(
      long attempted,
      long delivered,
      long failed,
      long retries,
      double successRate,
      double averageDeliverySeconds,
      double p95DeliverySeconds) {}

  public record SeriesPoint(
      Instant bucketAt,
      long attempted,
      long delivered,
      long failed,
      long retries,
      double successRate,
      double averageDeliverySeconds,
      double p95DeliverySeconds) {}

  public record ProviderRow(
      String provider,
      long attempted,
      long delivered,
      long failed,
      double successRate,
      String status,
      double averageResponseSeconds,
      double timeoutRate,
      String circuitBreakerState,
      int activeConnections,
      int maxConnections) {}

  public record ErrorRow(String errorCode, long failures, double share) {}

  public record QueueRow(
      Instant bucketAt,
      long pendingMessages,
      long normalPendingThreshold,
      long oldestMessageAgeSeconds,
      int activeConsumers,
      int expectedConsumers,
      long deadLetterCount,
      String processingRateStatus,
      String status) {}

  public record ChangeRow(
      String changeId,
      Instant occurredAt,
      String type,
      String component,
      String description,
      String version,
      Boolean approved) {}

  public record SampleRow(
      Instant bucketAt,
      String provider,
      long attempted,
      long delivered,
      long failed,
      long retries,
      double averageDeliverySeconds,
      double p95DeliverySeconds,
      String providerStatus,
      double timeoutRate,
      String errors) {}
}
