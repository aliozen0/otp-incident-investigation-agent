package com.example.otpsentinel.domain;

/** FR-017: the fixed set of events that must be audited. */
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
  INCIDENT_CREATED
}
