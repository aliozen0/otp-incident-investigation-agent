package com.example.otpsentinel.api.dto;

import java.util.List;

public record InvestigationResponseDto(
    String investigationId,
    String status,
    String severity,
    String summary,
    TimeWindowDto timeWindow,
    List<EvidenceDto> evidence,
    List<HypothesisDto> hypotheses,
    List<RecommendedActionDto> recommendedActions,
    List<KnowledgeReferenceDto> knowledgeReferences,
    double confidence,
    boolean approvalRequired,
    ValidationDto validation) {

  public record EvidenceDto(
      String id,
      String sourceType,
      String sourceReference,
      String observation,
      String observedAt) {}

  public record HypothesisDto(
      int rank,
      String possibleCause,
      String probability,
      List<String> supportingEvidenceIds,
      List<String> verificationSteps) {}

  public record RecommendedActionDto(
      String actionType, String description, String risk, boolean requiresApproval) {}

  public record KnowledgeReferenceDto(
      String documentId, String version, String chunkId, String title, Double similarityScore) {}

  public record ValidationDto(String status, List<String> warnings) {}
}
