package com.example.otpsentinel.agent;

import com.example.otpsentinel.tools.ToolResult;
import com.example.otpsentinel.tools.ToolStatus;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * Deterministic tool budget/dedup/timeout/retry, enforced in plain Java rather than a framework
 * guardrail (docs/09 "Core güvenlik yalnızca deneysel framework guardrail API'sine bağlı
 * bırakılmaz"). One instance per {@code Investigation} — not thread-safe, not reused across
 * investigations.
 *
 * <p>A tool result with {@link ToolStatus#TIMEOUT} (the fixture layer's deterministic timeout
 * simulation, NFR-008) is a legitimate business outcome and is not retried. Retry only applies to
 * an actual Java exception thrown by the invocation (a transient plumbing failure).
 */
public final class ToolBudgetGuard {

  private final int maxCalls;
  private final Duration toolTimeout;
  private final int retryCount;
  private final ExecutorService executor = Executors.newCachedThreadPool();
  private final List<CallRecord> calls = new ArrayList<>();
  private boolean policyLimitReached;

  public ToolBudgetGuard(int maxCalls, Duration toolTimeout, int retryCount) {
    if (maxCalls <= 0) {
      throw new IllegalStateException("maxCalls must be positive");
    }
    this.maxCalls = maxCalls;
    this.toolTimeout = Objects.requireNonNull(toolTimeout, "toolTimeout must not be null");
    if (retryCount < 0) {
      throw new IllegalStateException("retryCount must not be negative");
    }
    this.retryCount = retryCount;
  }

  public <T> ToolResult<T> execute(
      String toolName, Object parameters, Supplier<ToolResult<T>> invocation) {
    Objects.requireNonNull(toolName, "toolName must not be null");
    String paramKey = String.valueOf(parameters);

    boolean repeatsSuccess =
        calls.stream()
            .anyMatch(
                c -> c.toolName.equals(toolName) && c.paramKey.equals(paramKey) && c.succeeded);
    if (repeatsSuccess) {
      // Rejected, but not a policy breach: the model already has this result and repeating it costs
      // nothing but a wasted call. Callers turn this into a "reuse the earlier result" answer so a
      // clumsy repeat does not end an otherwise complete investigation.
      throw new DuplicateToolCallException(
          "duplicate successful call rejected: " + toolName + " " + paramKey);
    }
    if (calls.size() >= maxCalls) {
      policyLimitReached = true;
      throw new ToolBudgetExceededException("tool budget of " + maxCalls + " calls exceeded");
    }

    try {
      ToolResult<T> result = invokeWithTimeoutAndRetry(invocation);
      calls.add(new CallRecord(toolName, paramKey, result.status() == ToolStatus.SUCCESS));
      return result;
    } catch (RuntimeException e) {
      calls.add(new CallRecord(toolName, paramKey, false));
      throw e;
    }
  }

  private <T> ToolResult<T> invokeWithTimeoutAndRetry(Supplier<ToolResult<T>> invocation) {
    RuntimeException lastFailure = null;
    for (int attempt = 0; attempt <= retryCount; attempt++) {
      try {
        return callWithTimeout(invocation);
      } catch (RuntimeException e) {
        lastFailure = e;
      }
    }
    throw lastFailure;
  }

  private <T> ToolResult<T> callWithTimeout(Supplier<ToolResult<T>> invocation) {
    Future<ToolResult<T>> future = executor.submit(invocation::get);
    try {
      return future.get(toolTimeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
      future.cancel(true);
      throw new ToolTimeoutException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    } catch (java.util.concurrent.ExecutionException e) {
      Throwable cause = e.getCause();
      throw cause instanceof RuntimeException re ? re : new RuntimeException(cause);
    }
  }

  public int callCount() {
    return calls.size();
  }

  public List<String> toolNames() {
    return calls.stream().map(c -> c.toolName).toList();
  }

  /**
   * True when an invocation was rejected by budget or successful-call deduplication. LangChain4j
   * may turn tool exceptions into model-visible tool errors instead of propagating them, so the
   * application layer reads this deterministic state before accepting a final analysis.
   */
  public boolean policyLimitReached() {
    return policyLimitReached;
  }

  private record CallRecord(String toolName, String paramKey, boolean succeeded) {}
}
