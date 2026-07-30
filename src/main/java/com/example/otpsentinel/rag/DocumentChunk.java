package com.example.otpsentinel.rag;

/**
 * One chunk of a {@link KnowledgeDocument} produced by {@link Chunker} (docs/08-rag-spec.md
 * "Chunking başlangıç hipotezi"). {@code chunkId} is application-generated (ADR-008), never derived
 * from model output.
 */
public record DocumentChunk(
    String chunkId,
    String documentId,
    String version,
    String sectionTitle,
    String content,
    int tokenCount) {}
