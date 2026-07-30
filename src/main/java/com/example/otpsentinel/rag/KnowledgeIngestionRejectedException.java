package com.example.otpsentinel.rag;

/** A document failed a sanitization/allowlist gate and was never chunked, embedded or stored. */
public final class KnowledgeIngestionRejectedException extends RuntimeException {

  public KnowledgeIngestionRejectedException(String message) {
    super(message);
  }
}
