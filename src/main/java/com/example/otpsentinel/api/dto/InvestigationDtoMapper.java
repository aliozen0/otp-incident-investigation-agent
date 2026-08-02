package com.example.otpsentinel.api.dto;

import com.example.otpsentinel.domain.Evidence;
import com.example.otpsentinel.domain.Hypothesis;
import com.example.otpsentinel.domain.Investigation;
import com.example.otpsentinel.domain.RecommendedAction;

public final class InvestigationDtoMapper {

  private InvestigationDtoMapper() {}

  public static InvestigationResponseDto toDto(Investigation i) {
    return new InvestigationResponseDto(
        i.id().toString(),
        i.resultStatus() == null ? null : i.resultStatus().name(),
        i.severity() == null ? null : i.severity().name(),
        summary(i),
        new TimeWindowDto(i.resolvedTimeWindow().startAt(), i.resolvedTimeWindow().endAt(), "UTC"),
        i.evidence().stream().map(InvestigationDtoMapper::toEvidenceDto).toList(),
        i.hypotheses().stream().map(InvestigationDtoMapper::toHypothesisDto).toList(),
        i.recommendedActions().stream().map(InvestigationDtoMapper::toActionDto).toList(),
        i.knowledgeReferences().stream()
            .map(InvestigationResponseDto.KnowledgeReferenceDto::new)
            .toList(),
        i.confidence() == null ? 0.0 : i.confidence(),
        !i.recommendedActions().isEmpty()
            && i.recommendedActions().stream().anyMatch(RecommendedAction::requiresApproval),
        i.validationReport() == null
            ? null
            : new InvestigationResponseDto.ValidationDto(
                i.validationReport().status().name(), i.validationReport().warnings()));
  }

  private static String summary(Investigation i) {
    return i.resultStatus() == null ? "investigation in progress" : i.resultStatus().name();
  }

  private static InvestigationResponseDto.EvidenceDto toEvidenceDto(Evidence e) {
    return new InvestigationResponseDto.EvidenceDto(
        e.id(), e.sourceType(), e.sourceReference(), e.observation(), e.observedAt().toString());
  }

  private static InvestigationResponseDto.HypothesisDto toHypothesisDto(Hypothesis h) {
    return new InvestigationResponseDto.HypothesisDto(
        h.rank(),
        h.possibleCause(),
        String.valueOf(h.probability()),
        h.supportingEvidenceIds(),
        h.verificationSteps());
  }

  private static InvestigationResponseDto.RecommendedActionDto toActionDto(RecommendedAction a) {
    return new InvestigationResponseDto.RecommendedActionDto(
        a.actionType().name(), a.description(), a.risk().name(), a.requiresApproval());
  }
}
