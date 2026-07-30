package com.example.otpsentinel.rag;

import java.util.List;
import java.util.stream.Collectors;

/**
 * pgvector accepts its type as a text literal ({@code '[1,2,3]'::vector}); formatting one here
 * avoids pulling in the pgvector-java driver extension just for this.
 */
final class VectorLiterals {

  private VectorLiterals() {}

  static String toLiteral(List<Float> vector) {
    return vector.stream().map(String::valueOf).collect(Collectors.joining(",", "[", "]"));
  }
}
