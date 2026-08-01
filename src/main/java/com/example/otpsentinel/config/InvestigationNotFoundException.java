package com.example.otpsentinel.config;

/** Thrown when an {@code InvestigationId} does not resolve to a persisted investigation. */
public class InvestigationNotFoundException extends RuntimeException {

  public InvestigationNotFoundException(String message) {
    super(message);
  }
}
