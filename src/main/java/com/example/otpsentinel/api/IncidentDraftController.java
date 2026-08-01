package com.example.otpsentinel.api;

import com.example.otpsentinel.api.dto.IncidentDraftDecisionRequestDto;
import com.example.otpsentinel.api.dto.IncidentDraftDecisionResponseDto;
import com.example.otpsentinel.api.dto.IncidentDraftPreviewDto;
import com.example.otpsentinel.config.InvestigationOrchestrator;
import com.example.otpsentinel.domain.InvestigationId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/investigations/{investigationId}/incident-draft")
@Tag(
    name = "Incident Drafts",
    description = "Evidence-backed OTP incident investigation (mock/PoC — see README)")
public class IncidentDraftController {

  private final InvestigationOrchestrator orchestrator;

  public IncidentDraftController(InvestigationOrchestrator orchestrator) {
    this.orchestrator = orchestrator;
  }

  @PostMapping("/preview")
  @Operation(
      summary = "Preview an incident draft",
      description = "Kalıcı kayıt oluşturmadan taslak gösterir.")
  @ApiResponse(
      responseCode = "200",
      content =
          @Content(
              examples =
                  @ExampleObject(
                      value =
                          """
                          {
                            "title": "[HIGH] OTP delivery degradation on Operator B",
                            "severity": "HIGH",
                            "summary": "Son 15 dakikada OTP başarısı %72,1'e düştü.",
                            "evidenceCount": 6,
                            "recommendedChecks": ["Connection pool metriklerini incele", "Provider durumunu doğrula"],
                            "requiresExplicitApproval": true
                          }""")))
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
  @Operation(
      summary = "Approve or reject an incident draft",
      description =
          "Idempotent via the `Idempotency-Key` header: aynı key tekrarı 200, aynı ID ve"
              + " idempotentReplay=true.")
  @RequestBody(
      content =
          @Content(
              examples = {
                @ExampleObject(
                    name = "APPROVE",
                    value =
                        """
                        {
                          "decision": "APPROVE",
                          "reason": "Teknik ekip incelemesi için incident gerekli."
                        }"""),
                @ExampleObject(
                    name = "REJECT",
                    value =
                        """
                        {
                          "decision": "REJECT",
                          "reason": "Provider bakım duyurusu doğrulandı."
                        }""")
              }))
  public ResponseEntity<IncidentDraftDecisionResponseDto> decide(
      @PathVariable String investigationId,
      @org.springframework.web.bind.annotation.RequestBody IncidentDraftDecisionRequestDto request,
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
