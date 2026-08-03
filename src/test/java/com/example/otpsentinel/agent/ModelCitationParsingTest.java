package com.example.otpsentinel.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Live-model regression: a formatting slip in one citation used to make Jackson reject the entire
 * {@link IncidentAnalysisResult}, so a complete investigation was reported as FAILED.
 */
class ModelCitationParsingTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void readsEvidenceCitationsAsBareIdsAndAsObjects() throws Exception {
    List<EvidenceReference> parsed =
        mapper.readValue(
            "[\"ev-otp-success-rate-current\", {\"evidenceId\": \"ev-queue-health\"}]",
            new TypeReference<List<EvidenceReference>>() {});

    assertThat(parsed)
        .extracting(EvidenceReference::evidenceId)
        .containsExactly("ev-otp-success-rate-current", "ev-queue-health");
  }

  @Test
  void readsKnowledgeCitationsAsBareChunkIdsAndAsObjects() throws Exception {
    List<KnowledgeReference> parsed =
        mapper.readValue(
            "[\"doc-1#v1#c0\", {\"documentId\": \"doc-2\", \"chunkId\": \"doc-2#v1#c3\"}]",
            new TypeReference<List<KnowledgeReference>>() {});

    assertThat(parsed)
        .extracting(KnowledgeReference::documentId, KnowledgeReference::chunkId)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("doc-1", "doc-1#v1#c0"),
            org.assertj.core.groups.Tuple.tuple("doc-2", "doc-2#v1#c3"));
  }

  @Test
  void readsVisualizationPointValuesFromNumbersAndNumericStrings() throws Exception {
    List<VisualizationProposal.Point> parsed =
        mapper.readValue(
            """
            [{"label":"a","seriesKey":"rate","value":72.1,"evidenceId":"ev-1"},
             {"label":"b","seriesKey":"rate","value":"31.0","evidenceId":"ev-2"},
             {"label":"c","seriesKey":"rate","value":"Retry count changed from 3 to 2","evidenceId":"ev-3"}]
            """,
            new TypeReference<List<VisualizationProposal.Point>>() {});

    assertThat(parsed).extracting(VisualizationProposal.Point::value).containsExactly(72.1, 31.0, null);
  }

  @Test
  void dropsOnlyTheInvalidHypothesisInsteadOfTheWholeAnalysis() throws Exception {
    IncidentAnalysisResult parsed =
        mapper.readValue(
            """
            {"status":"ANOMALY_CONFIRMED","severity":"high","summary":"OTP başarı oranı düştü.",
             "evidence":["ev-1"],
             "hypotheses":[
               {"rank":1,"possibleCause":"Provider timeout","probability":0.8,
                "supportingEvidenceIds":["ev-1"],"verificationSteps":["Kontrol et"]},
               {"rank":2,"possibleCause":"Kanıtsız hipotez","probability":0.1,
                "supportingEvidenceIds":[]}],
             "recommendedActions":[
               {"actionType":"MANUAL_CHECK","description":"Sağlayıcıyı kontrol et","risk":"HIGH",
                "requiresApproval":false,"executionMode":"MANUAL_CHECK"}],
             "knowledgeReferences":[],"confidence":85}
            """,
            IncidentAnalysisResult.class);

    assertThat(parsed.hypotheses()).extracting(h -> h.possibleCause()).containsExactly("Provider timeout");
    assertThat(parsed.severity().name()).isEqualTo("HIGH");
    assertThat(parsed.confidence()).isEqualTo(0.85);
    // Invariant 6 is enforced rather than dropped: a HIGH risk action always requires approval.
    assertThat(parsed.recommendedActions()).singleElement().satisfies(action ->
        assertThat(action.requiresApproval()).isTrue());
  }

  @Test
  void hoistsPointsNestedUnderSeriesAndIgnoresUnknownFields() throws Exception {
    VisualizationProposal parsed =
        mapper.readValue(
            """
            {"id":"vis-1","type":"LINE","title":"OTP","unit":"RATIO","renderer":"chartjs",
             "series":[{"key":"current","label":"Current","points":[
               {"label":"Current","seriesKey":"current","value":0.681,"evidenceId":"ev-1"},
               {"label":"Previous","seriesKey":"current","value":0.721,"evidenceId":"ev-2"}]}]}
            """,
            VisualizationProposal.class);

    assertThat(parsed.series()).extracting(VisualizationProposal.Series::key).containsExactly("current");
    assertThat(parsed.points())
        .extracting(VisualizationProposal.Point::evidenceId, VisualizationProposal.Point::value)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("ev-1", 0.681),
            org.assertj.core.groups.Tuple.tuple("ev-2", 0.721));
  }
}
