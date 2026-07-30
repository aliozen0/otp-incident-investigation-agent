package com.example.otpsentinel.domain;

/** Process lifecycle of an {@link Investigation} (docs/05-domain-and-architecture.md). */
public enum InvestigationPhase {
  RECEIVED,
  COLLECTING_EVIDENCE,
  GENERATING_ANALYSIS,
  VALIDATING,
  COMPLETED,
  PARTIAL,
  FAILED
}
