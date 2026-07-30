package com.example.otpsentinel.rag;

/**
 * The fixed set of knowledge document types the RAG pipeline accepts (docs/08-rag-spec.md "Amaç").
 * Parsing an unknown type string into this enum IS the document-type allowlist control required by
 * docs/08 "Prompt injection koruması" — a type outside this set fails {@link #valueOf(String)} and
 * is rejected at ingestion, never reaching pgvector.
 */
public enum KnowledgeDocumentType {
  INCIDENT_POSTMORTEM,
  RUNBOOK,
  ERROR_REFERENCE,
  PROVIDER_PLAYBOOK,
  CHANGE_POLICY
}
