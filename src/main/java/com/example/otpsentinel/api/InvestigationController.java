package com.example.otpsentinel.api;

import com.example.otpsentinel.api.dto.InvestigationRequestDto;
import com.example.otpsentinel.api.dto.InvestigationResponseDto;
import com.example.otpsentinel.api.dto.TimeWindowDto;
import com.example.otpsentinel.config.InvestigationOrchestrator;
import com.example.otpsentinel.domain.Evidence;
import com.example.otpsentinel.domain.Hypothesis;
import com.example.otpsentinel.domain.Investigation;
import com.example.otpsentinel.domain.InvestigationId;
import com.example.otpsentinel.domain.RecommendedAction;
import com.example.otpsentinel.domain.TimeWindow;
import jakarta.servlet.http.HttpServletRequest;
import java.util.NoSuchElementException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/investigations")
public class InvestigationController {

  private final InvestigationOrchestrator orchestrator;
  private final InvestigationRequestValidator validator = new InvestigationRequestValidator();

  public InvestigationController(InvestigationOrchestrator orchestrator) {
    this.orchestrator = orchestrator;
  }

  @PostMapping
  public ResponseEntity<InvestigationResponseDto> create(
      @RequestBody InvestigationRequestDto request, HttpServletRequest httpRequest) {
    TimeWindow window = validator.validate(request);
    String correlationId = (String) httpRequest.getAttribute("correlationId");
    Investigation outcome =
        orchestrator.runInvestigation(request.question(), window, correlationId);
    return ResponseEntity.ok(toDto(outcome));
  }

  @GetMapping("/{id}")
  public ResponseEntity<InvestigationResponseDto> get(@PathVariable String id) {
    Investigation investigation =
        orchestrator
            .findInvestigation(InvestigationId.of(id))
            .orElseThrow(() -> new NoSuchElementException("investigation not found: " + id));
    return ResponseEntity.ok(toDto(investigation));
  }

  private static InvestigationResponseDto toDto(Investigation i) {
    return new InvestigationResponseDto(
        i.id().toString(),
        i.resultStatus() == null ? null : i.resultStatus().name(),
        i.severity() == null ? null : i.severity().name(),
        summary(i),
        new TimeWindowDto(i.resolvedTimeWindow().startAt(), i.resolvedTimeWindow().endAt(), "UTC"),
        i.evidence().stream().map(InvestigationController::toEvidenceDto).toList(),
        i.hypotheses().stream().map(InvestigationController::toHypothesisDto).toList(),
        i.recommendedActions().stream().map(InvestigationController::toActionDto).toList(),
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
