package com.example.otpsentinel.operations;

import com.example.otpsentinel.domain.TimeWindow;
import com.example.otpsentinel.tools.ChangeEvent;
import com.example.otpsentinel.tools.ErrorCount;
import com.example.otpsentinel.tools.ErrorDistributionRequest;
import com.example.otpsentinel.tools.ErrorDistributionResult;
import com.example.otpsentinel.tools.ErrorDistributionTool;
import com.example.otpsentinel.tools.OtpMetricsRequest;
import com.example.otpsentinel.tools.OtpMetricsResult;
import com.example.otpsentinel.tools.OtpMetricsTool;
import com.example.otpsentinel.tools.PeriodComparison;
import com.example.otpsentinel.tools.ProviderErrorBreakdown;
import com.example.otpsentinel.tools.ProviderHealthRequest;
import com.example.otpsentinel.tools.ProviderHealthResult;
import com.example.otpsentinel.tools.ProviderHealthTool;
import com.example.otpsentinel.tools.QueueHealthResult;
import com.example.otpsentinel.tools.QueueHealthTool;
import com.example.otpsentinel.tools.RecentChangesRequest;
import com.example.otpsentinel.tools.RecentChangesResult;
import com.example.otpsentinel.tools.RecentChangesTool;
import com.example.otpsentinel.tools.ToolError;
import com.example.otpsentinel.tools.ToolResult;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The five read-only tools answered from the operational tables instead of in-code fixtures, so the
 * numbers the agent cites are the same rows the console's data explorer renders and an operator can
 * re-aggregate by hand.
 */
public class JdbcOperationalDataTools
    implements OtpMetricsTool,
        ErrorDistributionTool,
        QueueHealthTool,
        ProviderHealthTool,
        RecentChangesTool {

  private final JdbcTemplate jdbcTemplate;
  private final Clock clock;

  public JdbcOperationalDataTools(JdbcTemplate jdbcTemplate, Clock clock) {
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
  }

  @Override
  public ToolResult<OtpMetricsResult> getOtpMetrics(OtpMetricsRequest request) {
    Map<String, Object> current = aggregate(request.startAt(), request.endAt());
    if (total(current) == 0) {
      return ToolResult.error(
          executionId(),
          "getOtpMetrics",
          clock.instant(),
          new ToolError("NO_DATA", "no OTP delivery samples in the requested window"));
    }
    PeriodComparison previous = null;
    if (request.includePreviousPeriod()) {
      Duration length = Duration.between(request.startAt(), request.endAt());
      Instant previousStart = request.startAt().minus(length);
      Map<String, Object> row = aggregate(previousStart, request.startAt());
      if (total(row) > 0) {
        previous =
            new PeriodComparison(
                new TimeWindow(previousStart, request.startAt()),
                total(row),
                successRate(row),
                averageSeconds(row));
      }
    }
    return ToolResult.success(
        executionId(),
        "getOtpMetrics",
        clock.instant(),
        new OtpMetricsResult(
            new TimeWindow(request.startAt(), request.endAt()),
            total(current),
            number(current, "delivered"),
            number(current, "failed"),
            successRate(current),
            averageSeconds(current),
            previous));
  }

  @Override
  public ToolResult<ErrorDistributionResult> getErrorDistribution(
      ErrorDistributionRequest request) {
    String provider = blankToNull(request.provider());
    List<ErrorRow> rows =
        jdbcTemplate.query(
            """
            SELECT error_code, SUM(failures) AS failures
            FROM otp_error_sample
            WHERE bucket_at >= ? AND bucket_at < ?
              AND (?::text IS NULL OR provider = ?::text)
            GROUP BY error_code
            ORDER BY failures DESC
            """,
            (rs, rowNum) -> new ErrorRow(rs.getString("error_code"), rs.getLong("failures")),
            Timestamp.from(request.startAt()),
            Timestamp.from(request.endAt()),
            provider,
            provider);
    long failedTotal = rows.stream().mapToLong(ErrorRow::failures).sum();
    if (failedTotal == 0) {
      return ToolResult.error(
          executionId(),
          "getErrorDistribution",
          clock.instant(),
          new ToolError("NO_DATA", "no OTP failures recorded in the requested window"));
    }
    List<ErrorCount> byErrorCode =
        rows.stream()
            .map(
                row ->
                    new ErrorCount(
                        row.errorCode(), row.failures(), round2(row.failures() * 100.0 / failedTotal)))
            .toList();

    List<ProviderErrorBreakdown> byProvider =
        jdbcTemplate.query(
            """
            SELECT provider, SUM(attempted) AS attempted, SUM(delivered) AS delivered,
                   SUM(failed) AS failed
            FROM otp_delivery_sample
            WHERE bucket_at >= ? AND bucket_at < ?
              AND (?::text IS NULL OR provider = ?::text)
            GROUP BY provider
            ORDER BY provider
            """,
            (rs, rowNum) -> {
              long attempted = rs.getLong("attempted");
              long delivered = rs.getLong("delivered");
              long failed = rs.getLong("failed");
              return new ProviderErrorBreakdown(
                  rs.getString("provider"),
                  attempted,
                  delivered,
                  failed,
                  attempted == 0 ? 0.0 : round2(delivered * 100.0 / attempted));
            },
            Timestamp.from(request.startAt()),
            Timestamp.from(request.endAt()),
            provider,
            provider);

    return ToolResult.success(
        executionId(),
        "getErrorDistribution",
        clock.instant(),
        new ErrorDistributionResult(failedTotal, byErrorCode, byProvider));
  }

  @Override
  public ToolResult<QueueHealthResult> getQueueHealth() {
    List<QueueHealthResult> rows =
        jdbcTemplate.query(
            """
            SELECT * FROM queue_health_sample ORDER BY bucket_at DESC LIMIT 1
            """,
            (rs, rowNum) ->
                new QueueHealthResult(
                    rs.getLong("pending_messages"),
                    rs.getLong("normal_pending_threshold"),
                    rs.getLong("oldest_message_age_seconds"),
                    rs.getLong("normal_oldest_age_threshold_seconds"),
                    rs.getInt("active_consumers"),
                    rs.getInt("expected_consumers"),
                    rs.getLong("dead_letter_count"),
                    rs.getString("processing_rate_status"),
                    rs.getString("status")));
    if (rows.isEmpty()) {
      return ToolResult.error(
          executionId(),
          "getQueueHealth",
          clock.instant(),
          new ToolError("NO_DATA", "no queue health samples recorded"));
    }
    return ToolResult.success(executionId(), "getQueueHealth", clock.instant(), rows.getFirst());
  }

  @Override
  public ToolResult<ProviderHealthResult> getProviderHealth(ProviderHealthRequest request) {
    String provider = normalizeProvider(request.provider());
    List<ProviderHealthResult> rows =
        jdbcTemplate.query(
            """
            SELECT * FROM provider_health_sample
            WHERE bucket_at >= ? AND bucket_at < ?
              AND (?::text IS NULL OR provider = ?::text)
            ORDER BY timeout_rate DESC, bucket_at DESC
            LIMIT 1
            """,
            (rs, rowNum) ->
                new ProviderHealthResult(
                    rs.getString("provider"),
                    rs.getString("status"),
                    rs.getDouble("avg_response_seconds"),
                    rs.getDouble("timeout_rate"),
                    rs.getTimestamp("last_successful_request_at") == null
                        ? null
                        : rs.getTimestamp("last_successful_request_at").toInstant(),
                    rs.getString("circuit_breaker_state"),
                    rs.getInt("active_connections"),
                    rs.getInt("max_connections")),
            Timestamp.from(request.startAt()),
            Timestamp.from(request.endAt()),
            provider,
            provider);
    if (rows.isEmpty()) {
      return ToolResult.error(
          executionId(),
          "getProviderHealth",
          clock.instant(),
          new ToolError(
              "NO_DATA", "no provider health samples for " + request.provider() + " in the window"));
    }
    return ToolResult.success(
        executionId(), "getProviderHealth", clock.instant(), rows.getFirst());
  }

  @Override
  public ToolResult<RecentChangesResult> getRecentChanges(RecentChangesRequest request) {
    String component = blankToNull(request.component());
    List<ChangeEvent> changes =
        jdbcTemplate.query(
            """
            SELECT * FROM change_event
            WHERE occurred_at >= ? AND occurred_at < ?
              AND (?::text IS NULL OR component = ?::text)
            ORDER BY occurred_at
            """,
            (rs, rowNum) ->
                new ChangeEvent(
                    rs.getString("change_id"),
                    rs.getTimestamp("occurred_at").toInstant(),
                    rs.getString("type"),
                    rs.getString("component"),
                    rs.getString("description"),
                    rs.getString("version"),
                    rs.getObject("approved") == null ? null : rs.getBoolean("approved")),
            Timestamp.from(request.from()),
            Timestamp.from(request.to()),
            component,
            component);
    return ToolResult.success(
        executionId(),
        "getRecentChanges",
        clock.instant(),
        new RecentChangesResult(changes));
  }

  private Map<String, Object> aggregate(Instant startAt, Instant endAt) {
    return jdbcTemplate.queryForMap(
        """
        SELECT COALESCE(SUM(attempted), 0) AS attempted,
               COALESCE(SUM(delivered), 0) AS delivered,
               COALESCE(SUM(failed), 0) AS failed,
               COALESCE(SUM(avg_delivery_seconds * attempted), 0) AS weighted_seconds
        FROM otp_delivery_sample
        WHERE bucket_at >= ? AND bucket_at < ?
        """,
        Timestamp.from(startAt),
        Timestamp.from(endAt));
  }

  private static long total(Map<String, Object> row) {
    return number(row, "attempted");
  }

  private static long number(Map<String, Object> row, String column) {
    Object value = row.get(column);
    return value instanceof Number n ? n.longValue() : 0L;
  }

  private static double successRate(Map<String, Object> row) {
    long attempted = number(row, "attempted");
    return attempted == 0 ? 0.0 : round2(number(row, "delivered") * 100.0 / attempted);
  }

  private static double averageSeconds(Map<String, Object> row) {
    long attempted = number(row, "attempted");
    Object weighted = row.get("weighted_seconds");
    double sum = weighted instanceof Number n ? n.doubleValue() : 0.0;
    return attempted == 0 ? 0.0 : round2(sum / attempted);
  }

  /**
   * Models ask for "ALL" or an empty provider when they want the overall picture. Treating that as
   * "no filter" and returning the worst-performing provider answers the question instead of failing
   * the tool call.
   */
  private static String normalizeProvider(String provider) {
    if (provider == null || provider.isBlank()) {
      return null;
    }
    String normalized = provider.trim();
    String upper = normalized.toUpperCase(Locale.ROOT);
    return switch (upper) {
      case "ALL", "ANY", "*", "NULL", "NONE", "DEFAULT", "UNDEFINED", "UNKNOWN", "STRING", "-" ->
          null;
      default -> normalized;
    };
  }

  /** Same placeholder tolerance as the provider filter: "all"/"default" means "no filter". */
  private static String blankToNull(String value) {
    return normalizeProvider(value);
  }

  private static double round2(double value) {
    return Math.round(value * 100.0) / 100.0;
  }

  private static String executionId() {
    return UUID.randomUUID().toString();
  }

  private record ErrorRow(String errorCode, long failures) {}
}
