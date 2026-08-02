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
        knowledgeReferences(i),
        i.confidence() == null ? 0.0 : i.confidence(),
        !i.recommendedActions().isEmpty()
            && i.recommendedActions().stream().anyMatch(RecommendedAction::requiresApproval),
        i.validationReport() == null
            ? null
            : new InvestigationResponseDto.ValidationDto(
                i.validationReport().status().name(), i.validationReport().warnings()),
        i.visualizations().stream().map(InvestigationDtoMapper::toVisualizationDto).toList());
  }

  private static String summary(Investigation i) {
    return i.summary() != null
        ? i.summary()
        : i.resultStatus() == null ? "investigation in progress" : i.resultStatus().name();
  }

  private static java.util.List<InvestigationResponseDto.KnowledgeReferenceDto> knowledgeReferences(
      Investigation investigation) {
    if (!investigation.knowledgeCitations().isEmpty()) {
      return investigation.knowledgeCitations().stream()
          .map(
              citation ->
                  new InvestigationResponseDto.KnowledgeReferenceDto(
                      citation.documentId(),
                      citation.version(),
                      citation.chunkId(),
                      citation.title(),
                      citation.similarityScore()))
          .toList();
    }
    return investigation.knowledgeReferences().stream()
        .map(
            documentId ->
                new InvestigationResponseDto.KnowledgeReferenceDto(
                    documentId, null, null, null, null))
        .toList();
  }

  private static InvestigationResponseDto.EvidenceDto toEvidenceDto(Evidence e) {
    return new InvestigationResponseDto.EvidenceDto(
        e.id(),
        e.sourceType(),
        e.sourceReference(),
        e.observation(),
        e.observedAt().toString(),
        e.metricName(),
        e.metricValue(),
        e.metricUnit());
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

  private static InvestigationResponseDto.VisualizationDto toVisualizationDto(
      com.example.otpsentinel.domain.VisualizationSpec visualization) {
    return new InvestigationResponseDto.VisualizationDto(
        visualization.id(),
        visualization.type().name(),
        visualization.title(),
        visualization.xAxisLabel(),
        visualization.yAxisLabel(),
        visualization.unit().name(),
        visualization.series().stream()
            .map(
                item ->
                    new InvestigationResponseDto.VisualizationSeriesDto(
                        item.key(), item.label()))
            .toList(),
        visualization.points().stream()
            .map(
                item ->
                    new InvestigationResponseDto.VisualizationPointDto(
                        item.label(), item.seriesKey(), item.value(), item.evidenceId()))
            .toList());
  }
}
