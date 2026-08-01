package com.example.otpsentinel.config;

/**
 * Thrown when a preview/decision is requested for an investigation that has not reached {@code
 * COMPLETED} with a passed validation report.
 */
public class InvestigationNotActionableException extends RuntimeException {

  public InvestigationNotActionableException(String message) {
    super(message);
  }
}
