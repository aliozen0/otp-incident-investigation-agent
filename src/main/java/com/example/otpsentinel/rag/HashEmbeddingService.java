package com.example.otpsentinel.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Deterministic hashing-trick bag-of-words embedding — no NVIDIA call, so knowledge-document
 * ingestion works even without {@code NVIDIA_API_KEY} (docs/09 constraint carried into M11's
 * document-upload endpoint). Every token increments a fixed-size bucket, then the vector is
 * L2-normalized, so cosine similarity tracks vocabulary overlap. Deterministic and good enough for
 * offline/demo search; {@code AI_MODE=live} uses {@link NvidiaNimEmbeddingService} instead for real
 * embedding quality.
 */
public final class HashEmbeddingService implements EmbeddingService {

  private final int dimension;

  public HashEmbeddingService(int dimension) {
    this.dimension = dimension;
  }

  @Override
  public List<Float> embed(String text, EmbeddingInputType inputType) {
    float[] vector = new float[dimension];
    for (String token : text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{Nd}]+")) {
      if (token.isBlank()) {
        continue;
      }
      int index = Math.floorMod(token.hashCode(), dimension);
      vector[index] += 1f;
    }
    double norm = 0;
    for (float v : vector) {
      norm += (double) v * v;
    }
    norm = Math.sqrt(norm);
    List<Float> result = new ArrayList<>(dimension);
    for (float v : vector) {
      result.add(norm == 0 ? 0f : (float) (v / norm));
    }
    return result;
  }

  @Override
  public int dimension() {
    return dimension;
  }

  @Override
  public String modelId() {
    return "hash-embedding-v1";
  }
}
