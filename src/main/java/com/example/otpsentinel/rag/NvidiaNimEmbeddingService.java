package com.example.otpsentinel.rag;

import dev.langchain4j.model.embedding.request.EmbeddingRequest;
import dev.langchain4j.model.embedding.response.EmbeddingResponse;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingRequestParameters;
import java.util.List;
import java.util.Objects;

/**
 * {@link EmbeddingService} over NVIDIA NIM's OpenAI-compatible embeddings endpoint (docs/16
 * ADR-015), via LangChain4j's {@link OpenAiEmbeddingModel}.
 *
 * <p>NIM's embedding models require an extra {@code input_type} (query/passage) body field the
 * standard OpenAI schema does not have. LangChain4j 1.18+ added {@link
 * OpenAiEmbeddingRequestParameters#CUSTOM_PARAMETERS} for exactly this passthrough case (its
 * Javadoc names NVIDIA's {@code input_type} explicitly), so no custom HTTP interceptor is needed —
 * confirmed by the compatibility spike in {@code NvidiaNimEmbeddingServiceLiveTest}.
 */
public final class NvidiaNimEmbeddingService implements EmbeddingService {

  private final OpenAiEmbeddingModel model;
  private final String modelId;
  private final int dimension;

  public NvidiaNimEmbeddingService(String baseUrl, String apiKey, String modelId, int dimension) {
    if (modelId == null || modelId.isBlank()) {
      throw new IllegalArgumentException("modelId must not be blank (NVIDIA_EMBEDDING_MODEL)");
    }
    this.modelId = modelId;
    if (dimension <= 0) {
      throw new IllegalArgumentException("dimension must be positive");
    }
    this.dimension = dimension;
    this.model =
        OpenAiEmbeddingModel.builder()
            .baseUrl(Objects.requireNonNull(baseUrl, "baseUrl must not be null"))
            .apiKey(Objects.requireNonNull(apiKey, "apiKey must not be null"))
            .modelName(modelId)
            .build();
  }

  @Override
  public List<Float> embed(String text, EmbeddingInputType inputType) {
    Objects.requireNonNull(text, "text must not be null");
    Objects.requireNonNull(inputType, "inputType must not be null");

    EmbeddingRequest request =
        EmbeddingRequest.builder()
            .input(text)
            .parameters(
                OpenAiEmbeddingRequestParameters.builder()
                    .customParameter("input_type", nvidiaInputType(inputType))
                    .build())
            .build();

    EmbeddingResponse response = model.embed(request);
    return response.embeddings().get(0).vectorAsList();
  }

  @Override
  public int dimension() {
    return dimension;
  }

  @Override
  public String modelId() {
    return modelId;
  }

  private static String nvidiaInputType(EmbeddingInputType inputType) {
    return switch (inputType) {
      case QUERY -> "query";
      case PASSAGE -> "passage";
    };
  }
}
