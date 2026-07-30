package com.example.otpsentinel.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * {@link EmbeddingService} test double for the main suite (prompts/handoff/M4-prompt.md "Kısıtlar":
 * no NVIDIA key needed). A hashing-trick bag-of-words vectorizer: every token increments a
 * fixed-size bucket, then the vector is L2-normalized, so cosine similarity tracks vocabulary
 * overlap — deterministic and good enough to rank docs/08's evaluation-set queries correctly
 * against the fixture documents, without calling anything live.
 */
final class DeterministicHashEmbeddingService implements EmbeddingService {

  private final int dimension;

  DeterministicHashEmbeddingService(int dimension) {
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
    return "test-deterministic-hash-v1";
  }
}
