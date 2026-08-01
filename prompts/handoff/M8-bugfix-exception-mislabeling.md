# M8 Bugfix: Stop Mislabeling Internal Wiring Bugs as Client 400s

**Parked finding from:** M7 final review  
**Commit:** fb96e38

## Symptom

Two internal-only exception throw sites were using `IllegalArgumentException`, causing them to be caught by `GlobalExceptionHandler`'s broad `@ExceptionHandler(IllegalArgumentException.class)` and mislabeled as `400 INVALID_REQUEST` errors. These exceptions can only be thrown by server-side bugs (broken deployment/config), never by a client request, so they should surface as `500` instead.

## Root Cause

1. `IncidentInvestigationService.requireMatchingRequest()` throws when `InvestigationOrchestrator` builds a mismatched `InvestigationRequest`/`Investigation` pair internally — a server wiring bug, not a client input validation failure.
2. `ToolBudgetGuard` constructor throws when `otp-sentinel.ai.max-tool-calls` or `otp-sentinel.tool.retry-count` are misconfigured at startup — a config bug, not a client request error.

Both were using `IllegalArgumentException`, which is conventionally thrown by input validation but here represents internal invariant violations that should be `IllegalStateException`.

## Fix

Changed both throw sites from `IllegalArgumentException` to `IllegalStateException`:
- `IncidentInvestigationService.java:204` in `requireMatchingRequest()`
- `ToolBudgetGuard.java:37, 42` in constructor guards

The existing `GlobalExceptionHandler`'s `IllegalArgumentException` handler remains untouched. These two exceptions now correctly fall through to Spring's default 500 handler.

## Files Changed

1. `src/main/java/com/example/otpsentinel/application/IncidentInvestigationService.java`
   - Line 204: Changed `throw new IllegalArgumentException(...)` to `throw new IllegalStateException(...)`

2. `src/main/java/com/example/otpsentinel/agent/ToolBudgetGuard.java`
   - Line 37: Changed `throw new IllegalArgumentException("maxCalls must be positive")` to `throw new IllegalStateException("maxCalls must be positive")`
   - Line 42: Changed `throw new IllegalArgumentException("retryCount must not be negative")` to `throw new IllegalStateException("retryCount must not be negative")`

3. `src/test/java/com/example/otpsentinel/application/IncidentInvestigationServiceTest.java`
   - Added test: `mismatchedRequestIsAnInternalWiringBugNotAClientError()` — verifies that a mismatched request throws `IllegalStateException`

4. `src/test/java/com/example/otpsentinel/agent/ToolBudgetGuardTest.java`
   - Added test: `rejectsNonPositiveMaxCallsAsAnInternalConfigBug()` — verifies that invalid maxCalls throws `IllegalStateException`
   - Added test: `rejectsNegativeRetryCountAsAnInternalConfigBug()` — verifies that negative retryCount throws `IllegalStateException`

## Test Evidence

All three new tests now pass, confirming the fix:

```
[INFO] Tests run: 19, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 19, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
```

**Before fix:** Tests failed with:
```
java.lang.AssertionError: 
Expecting actual throwable to be an instance of:
  java.lang.IllegalStateException
but was:
  java.lang.IllegalArgumentException: request does not match investigation
```

**After fix:** All tests pass, confirming the exception type changed as expected.

## Impact

- **Backward compatibility:** No breaking changes. This is purely a fix to exception classification (500 vs. 400), which only affects how internal bugs surface to clients.
- **Error handling:** The broad `IllegalArgumentException -> 400` handler in `GlobalExceptionHandler` now only catches legitimate input validation failures, improving error classification accuracy.
- **Verification:** Grep-verified that the 24 other `IllegalArgumentException` throw sites in the codebase are either genuine per-request input validation (correctly 400) or caught internally before reaching the controller.
