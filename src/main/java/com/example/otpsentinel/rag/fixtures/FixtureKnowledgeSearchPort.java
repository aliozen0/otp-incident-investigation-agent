package com.example.otpsentinel.rag.fixtures;

import com.example.otpsentinel.rag.KnowledgeSearchPort;
import com.example.otpsentinel.rag.KnowledgeSearchResult;
import java.util.List;

/**
 * Deterministic offline substitute for {@link
 * com.example.otpsentinel.rag.JdbcKnowledgeSearchAdapter} when {@code AI_MODE=stub} (no embedding
 * model available). Always returns the OTP-DROP-001 demo fixture's single INC-2026-041 citation,
 * matching docs/15-demo-fixtures.md.
 */
public final class FixtureKnowledgeSearchPort implements KnowledgeSearchPort {

  @Override
  public List<KnowledgeSearchResult> searchIncidentKnowledge(
      String queryText, String providerFilter, int topK) {
    return List.of(
        new KnowledgeSearchResult(
            "INC-2026-041",
            "1",
            "Connection pool exhaustion incident",
            "INC-2026-041#v1#c0",
            0.85,
            "When active connections approach max and timeout rate rises, suspect connection pool"
                + " exhaustion."));
  }
}
