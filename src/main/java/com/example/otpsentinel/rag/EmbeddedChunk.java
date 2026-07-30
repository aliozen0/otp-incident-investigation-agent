package com.example.otpsentinel.rag;

import java.util.List;

/** A {@link DocumentChunk} paired with its embedding vector and the model that produced it. */
public record EmbeddedChunk(DocumentChunk chunk, List<Float> embedding, String embeddingModel) {}
