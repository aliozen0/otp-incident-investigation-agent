package com.example.otpsentinel.rag;

import java.util.List;

/**
 * Port for T-006 {@code searchIncidentKnowledge} (docs/08-rag-spec.md "Retrieval pipeline"). Not
 * yet bound as a LangChain4j {@code @Tool} — that wiring is M5's (prompts/handoff/M4-prompt.md).
 */
public interface KnowledgeSearchPort {

  /**
   * @param providerFilter restricts results to this provider's documents when non-null (docs/08
   *     "provider biliniyorsa filter"); documents with no provider are still included.
   * @param topK upper bound on returned results; the port additionally never exceeds 5 (docs/08
   *     "topK=5") and never returns a result below the configured minimum similarity.
   */
  List<KnowledgeSearchResult> searchIncidentKnowledge(
      String queryText, String providerFilter, int topK);

  /**
   * Diagnostic ranking for the console's retrieval explorer: same query, same ordering, but without
   * the agent's minimum-similarity floor. An operator checking what the knowledge base contains
   * needs to see the near misses and their scores — silently returning nothing reads as "search is
   * broken". The agent itself keeps using {@link #searchIncidentKnowledge}.
   */
  default List<KnowledgeSearchResult> previewIncidentKnowledge(
      String queryText, String providerFilter, int topK) {
    return searchIncidentKnowledge(queryText, providerFilter, topK);
  }
}
