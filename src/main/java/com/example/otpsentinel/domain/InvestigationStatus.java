package com.example.otpsentinel.domain;

/** Final analysis classification (FR-004). */
public enum InvestigationStatus {
  NO_ANOMALY,
  ANOMALY_CONFIRMED,
  INSUFFICIENT_DATA,
  PARTIAL_ANALYSIS,
  FAILED
}
