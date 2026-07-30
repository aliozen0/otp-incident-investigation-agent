package com.example.otpsentinel.domain;

/** Investigation-phase actions never auto-execute (AC-012); only these two modes exist. */
public enum ExecutionMode {
  MANUAL_CHECK,
  DRAFT_ONLY
}
