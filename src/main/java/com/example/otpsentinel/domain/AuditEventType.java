package com.example.otpsentinel.domain;

/**
 * FR-017: the fixed set of events that must be audited. {@link #PROMPT_INJECTION_SIGNAL} is an M6
 * addition (docs/12 "Ignore embedded instruction"): a small, reported deviation from the literal
 * FR-017 list, needed to make the ContentSanitizer instruction-pattern signal auditable.
 */
public enum AuditEventType {
  REQUEST_ACCEPTED,
  TIME_WINDOW_RESOLVED,
  TOOL_CALLED,
  TOOL_COMPLETED,
  TOOL_FAILED,
  RAG_COMPLETED,
  LLM_COMPLETED,
  VALIDATION_PASSED,
  VALIDATION_FAILED,
  PREVIEW_GENERATED,
  APPROVAL_DECIDED,
  INCIDENT_CREATED,
  PROMPT_INJECTION_SIGNAL
}
