package com.example.otpsentinel.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.otpsentinel.tools.ToolError;
import com.example.otpsentinel.tools.ToolResult;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ToolBudgetGuardTest {

  private ToolResult<String> success(String executionId) {
    return ToolResult.success(executionId, "getOtpMetrics", Instant.now(), "data");
  }

  @Test
  void allowsCallsUpToTheBudget() {
    ToolBudgetGuard guard = new ToolBudgetGuard(2, Duration.ofSeconds(2), 1);
    guard.execute("getOtpMetrics", "params-a", () -> success("exec-1"));
    guard.execute("getErrorDistribution", "params-b", () -> success("exec-2"));
    assertThat(guard.callCount()).isEqualTo(2);
  }

  @Test
  void rejectsCallsPastTheBudget() {
    ToolBudgetGuard guard = new ToolBudgetGuard(1, Duration.ofSeconds(2), 1);
    guard.execute("getOtpMetrics", "params-a", () -> success("exec-1"));
    assertThatThrownBy(() -> guard.execute("getQueueHealth", "params-b", () -> success("exec-2")))
        .isInstanceOf(ToolBudgetExceededException.class);
    assertThat(guard.policyLimitReached()).isTrue();
  }

  @Test
  void rejectsRepeatingASuccessfulSameToolSameParamsCall() {
    ToolBudgetGuard guard = new ToolBudgetGuard(8, Duration.ofSeconds(2), 1);
    guard.execute("getOtpMetrics", "params-a", () -> success("exec-1"));
    assertThatThrownBy(() -> guard.execute("getOtpMetrics", "params-a", () -> success("exec-2")))
        .isInstanceOf(DuplicateToolCallException.class);
    // A repeat is refused but is not a policy limit: the run continues with the earlier result.
    assertThat(guard.policyLimitReached()).isFalse();
  }

  @Test
  void allowsRetryingWithSameParamsAfterAFailedCall() {
    ToolBudgetGuard guard = new ToolBudgetGuard(8, Duration.ofSeconds(2), 1);
    ToolResult<String> failed =
        ToolResult.error(
            "exec-1", "getProviderHealth", Instant.now(), new ToolError("ERR", "boom"));
    guard.execute("getProviderHealth", "params-a", () -> failed);
    ToolResult<String> result =
        guard.execute("getProviderHealth", "params-a", () -> success("exec-2"));
    assertThat(result.executionId()).isEqualTo("exec-2");
  }

  @Test
  void retriesOnceOnTransientThrowThenSucceeds() {
    ToolBudgetGuard guard = new ToolBudgetGuard(8, Duration.ofSeconds(2), 1);
    AtomicInteger attempts = new AtomicInteger();
    ToolResult<String> result =
        guard.execute(
            "getQueueHealth",
            "params-a",
            () -> {
              if (attempts.getAndIncrement() == 0) {
                throw new RuntimeException("transient");
              }
              return success("exec-1");
            });
    assertThat(result.executionId()).isEqualTo("exec-1");
    assertThat(attempts.get()).isEqualTo(2);
  }

  @Test
  void givesUpAfterOneRetry() {
    ToolBudgetGuard guard = new ToolBudgetGuard(8, Duration.ofSeconds(2), 1);
    assertThatThrownBy(
            () ->
                guard.execute(
                    "getQueueHealth",
                    "params-a",
                    () -> {
                      throw new RuntimeException("always fails");
                    }))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("always fails");
  }

  @Test
  void countsAndAllowsRetryOfACallThatExhaustsRetriesAndThrows() {
    ToolBudgetGuard guard = new ToolBudgetGuard(2, Duration.ofSeconds(2), 1);
    assertThatThrownBy(
            () ->
                guard.execute(
                    "getQueueHealth",
                    "params-a",
                    () -> {
                      throw new RuntimeException("always fails");
                    }))
        .isInstanceOf(RuntimeException.class);
    assertThat(guard.callCount()).isEqualTo(1);

    // Not blocked by dedup (the failed call didn't succeed), and counts toward the budget.
    ToolResult<String> result =
        guard.execute("getQueueHealth", "params-a", () -> success("exec-1"));
    assertThat(result.executionId()).isEqualTo("exec-1");
    assertThat(guard.callCount()).isEqualTo(2);

    // Budget is now exhausted (maxCalls=2); use different params so dedup isn't what trips it.
    assertThatThrownBy(() -> guard.execute("getQueueHealth", "params-b", () -> success("exec-2")))
        .isInstanceOf(ToolBudgetExceededException.class);
  }

  @Test
  void timesOutAfterConfiguredDuration() {
    ToolBudgetGuard guard = new ToolBudgetGuard(8, Duration.ofMillis(100), 0);
    assertThatThrownBy(
            () ->
                guard.execute(
                    "getQueueHealth",
                    "params-a",
                    () -> {
                      try {
                        Thread.sleep(500);
                      } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                      }
                      return success("exec-1");
                    }))
        .isInstanceOf(ToolTimeoutException.class);
  }

  @Test
  void rejectsNonPositiveMaxCallsAsAnInternalConfigBug() {
    assertThatThrownBy(() -> new ToolBudgetGuard(0, Duration.ofSeconds(2), 1))
        .isInstanceOf(IllegalStateException.class)
        .isNotInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNegativeRetryCountAsAnInternalConfigBug() {
    assertThatThrownBy(() -> new ToolBudgetGuard(1, Duration.ofSeconds(2), -1))
        .isInstanceOf(IllegalStateException.class)
        .isNotInstanceOf(IllegalArgumentException.class);
  }
}
