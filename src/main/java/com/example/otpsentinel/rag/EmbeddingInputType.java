package com.example.otpsentinel.rag;

/**
 * Asymmetric embedding intent: a knowledge chunk is embedded once as {@link #PASSAGE} at ingestion,
 * a user query is embedded as {@link #QUERY} at retrieval time (docs/08-rag-spec.md "Embedding
 * kuralları" — NVIDIA NIM {@code input_type}, docs/16 ADR-015).
 */
public enum EmbeddingInputType {
  QUERY,
  PASSAGE
}
