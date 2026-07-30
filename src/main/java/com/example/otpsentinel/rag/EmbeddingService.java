package com.example.otpsentinel.rag;

import java.util.List;

/**
 * Port for turning text into a vector, independent of both the agent/tool-calling layer and any
 * particular embedding provider (this milestone: rag package stays framework-independent per
 * prompts/handoff/M4-prompt.md; only the adapter implementation touches LangChain4j).
 */
public interface EmbeddingService {

  /** Embeds {@code text}; a query and a passage embedding are not comparable across models. */
  List<Float> embed(String text, EmbeddingInputType inputType);

  /**
   * Vector dimension this service always returns; must match the {@code knowledge_chunk.embedding}
   * column.
   */
  int dimension();

  /** Identifier persisted alongside every chunk (DATA-004), so a model change is detectable. */
  String modelId();
}
