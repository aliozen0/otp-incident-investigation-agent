package com.example.otpsentinel.api;

import com.example.otpsentinel.api.dto.IncidentDraftDecisionRequestDto;
import com.example.otpsentinel.api.dto.IncidentDraftDecisionResponseDto;
import com.example.otpsentinel.api.dto.IncidentDraftPreviewDto;
import com.example.otpsentinel.config.InvestigationOrchestrator;
import com.example.otpsentinel.domain.InvestigationId;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/investigations/{investigationId}/incident-draft")
public class IncidentDraftController {

  private final InvestigationOrchestrator orchestrator;

  public IncidentDraftController(InvestigationOrchestrator orchestrator) {
    this.orchestrator = orchestrator;
  }

  @PostMapping("/preview")
  public ResponseEntity<IncidentDraftPreviewDto> preview(
      @PathVariable String investigationId, HttpServletRequest request) {
    String correlationId = (String) request.getAttribute("correlationId");
    InvestigationOrchestrator.IncidentDraftPreview preview =
        orchestrator.previewIncidentDraft(InvestigationId.of(investigationId), correlationId);
    return ResponseEntity.ok(
        new IncidentDraftPreviewDto(
            preview.title(),
            preview.severity().name(),
            preview.summary(),
            preview.evidenceCount(),
            preview.recommendedChecks(),
            preview.requiresExplicitApproval()));
  }

  @PostMapping("/decisions")
  public ResponseEntity<IncidentDraftDecisionResponseDto> decide(
      @PathVariable String investigationId,
      @RequestBody IncidentDraftDecisionRequestDto request,
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      HttpServletRequest httpRequest) {
    String correlationId = (String) httpRequest.getAttribute("correlationId");
    var outcome =
        orchestrator.decide(
            InvestigationId.of(investigationId),
            request.decision(),
            request.reason(),
            idempotencyKey,
            correlationId);
    var body =
        new IncidentDraftDecisionResponseDto(
            outcome.incidentDraftId().toString(),
            outcome.externalIncidentId(),
            outcome.status().name(),
            outcome.idempotentReplay());
    var status = outcome.idempotentReplay() ? HttpStatus.OK : HttpStatus.CREATED;
    return ResponseEntity.status(status).body(body);
  }
}
