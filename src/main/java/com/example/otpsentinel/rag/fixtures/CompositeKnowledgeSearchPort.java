package com.example.otpsentinel.rag.fixtures;

import com.example.otpsentinel.rag.KnowledgeSearchPort;
import com.example.otpsentinel.rag.KnowledgeSearchResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Non-live ({@code AI_MODE != live}) knowledge search: the deterministic demo fixture citation PLUS
 * whatever really was ingested into pgvector — documents uploaded through {@code POST
 * /api/v1/knowledge/documents} would otherwise be invisible to the agent in the only mode the demo
 * and the test suite actually run in (M11 finding 4).
 *
 * <p>Results are deduplicated by {@code documentId}, ordered by similarity descending and capped to
 * {@code topK}. The fixture delegate keeps its unconditional result so the deterministic
 * OTP-DROP-001 script's {@code INC-2026-041} citation still survives {@code
 * IncidentInvestigationService}'s known-reference filter.
 */
public final class CompositeKnowledgeSearchPort implements KnowledgeSearchPort {

  private static final int FALLBACK_TOP_K = 5;

  private final KnowledgeSearchPort fixtureDelegate;
  private final KnowledgeSearchPort ingestedDelegate;

  public CompositeKnowledgeSearchPort(
      KnowledgeSearchPort fixtureDelegate, KnowledgeSearchPort ingestedDelegate) {
    this.fixtureDelegate = Objects.requireNonNull(fixtureDelegate, "fixtureDelegate");
    this.ingestedDelegate = Objects.requireNonNull(ingestedDelegate, "ingestedDelegate");
  }

  @Override
  public List<KnowledgeSearchResult> searchIncidentKnowledge(
      String queryText, String providerFilter, int topK) {
    List<KnowledgeSearchResult> merged =
        new ArrayList<>(fixtureDelegate.searchIncidentKnowledge(queryText, providerFilter, topK));
    merged.addAll(ingestedDelegate.searchIncidentKnowledge(queryText, providerFilter, topK));
    merged.sort(Comparator.comparingDouble(KnowledgeSearchResult::similarityScore).reversed());

    Map<String, KnowledgeSearchResult> byDocumentId = new LinkedHashMap<>();
    for (KnowledgeSearchResult result : merged) {
      byDocumentId.putIfAbsent(result.documentId(), result);
    }
    return byDocumentId.values().stream().limit(topK <= 0 ? FALLBACK_TOP_K : topK).toList();
  }
}
