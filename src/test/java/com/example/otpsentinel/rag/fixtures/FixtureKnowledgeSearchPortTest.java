package com.example.otpsentinel.rag.fixtures;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.otpsentinel.rag.KnowledgeSearchResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class FixtureKnowledgeSearchPortTest {

  @Test
  void returnsDeterministicIncidentPostmortemResult() {
    FixtureKnowledgeSearchPort port = new FixtureKnowledgeSearchPort();

    List<KnowledgeSearchResult> results =
        port.searchIncidentKnowledge(
            "OTP success rate drop connection pool timeout", "OPERATOR_B", 5);

    assertThat(results).hasSize(1);
    KnowledgeSearchResult result = results.get(0);
    assertThat(result.documentId()).isEqualTo("INC-2026-041");
    assertThat(result.chunkId()).isEqualTo("INC-2026-041#v1#c0");
    assertThat(result.similarityScore()).isEqualTo(0.85);
  }
}
