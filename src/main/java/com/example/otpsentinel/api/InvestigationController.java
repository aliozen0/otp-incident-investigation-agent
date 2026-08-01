package com.example.otpsentinel.api;

import com.example.otpsentinel.api.dto.InvestigationRequestDto;
import com.example.otpsentinel.api.dto.InvestigationResponseDto;
import com.example.otpsentinel.api.dto.TimeWindowDto;
import com.example.otpsentinel.config.InvestigationNotFoundException;
import com.example.otpsentinel.config.InvestigationOrchestrator;
import com.example.otpsentinel.domain.Evidence;
import com.example.otpsentinel.domain.Hypothesis;
import com.example.otpsentinel.domain.Investigation;
import com.example.otpsentinel.domain.InvestigationId;
import com.example.otpsentinel.domain.RecommendedAction;
import com.example.otpsentinel.domain.TimeWindow;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/investigations")
@Tag(
    name = "Investigations",
    description = "Evidence-backed OTP incident investigation (mock/PoC — see README)")
public class InvestigationController {

  private final InvestigationOrchestrator orchestrator;
  private final InvestigationRequestValidator validator = new InvestigationRequestValidator();

  public InvestigationController(InvestigationOrchestrator orchestrator) {
    this.orchestrator = orchestrator;
  }

  @PostMapping
  @Operation(
      summary = "Start an evidence-backed investigation",
      description =
          "Collects OTP metrics/errors/queue/provider/change evidence via tool calling, "
              + "retrieves similar past incidents via RAG, and returns a structured, validated"
              + " analysis.")
  @RequestBody(
      content =
          @Content(
              examples =
                  @ExampleObject(
                      name = "OTP-DROP-001",
                      value =
                          """
                          {
                            "question": "Son 15 dakikada OTP teslimat oranı neden düştü?",
                            "timeWindow": {"startAt": "2026-07-30T11:15:00Z", "endAt": "2026-07-30T11:30:00Z"},
                            "locale": "tr-TR"
                          }""")))
  @ApiResponse(
      responseCode = "200",
      content =
          @Content(
              examples =
                  @ExampleObject(
                      value =
                          """
                          {
                            "investigationId": "2c321a4e-178f-4f68-b705-18f188d73e75",
                            "status": "ANOMALY_CONFIRMED",
                            "severity": "HIGH",
                            "summary": "OTP başarısı %98,1'den %72,1'e düştü ve başarısızlıklar Operatör B üzerinde yoğunlaştı.",
                            "timeWindow": {
                              "startAt": "2026-07-30T11:15:00Z",
                              "endAt": "2026-07-30T11:30:00Z",
                              "timezone": "UTC"
                            },
                            "evidence": [
                              {
                                "id": "ev-provider-rate",
                                "sourceType": "PROVIDER_HEALTH",
                                "sourceReference": "tool:getProviderHealth:exec-5",
                                "observation": "Operatör B timeout oranı %31 ve circuit breaker HALF_OPEN.",
                                "observedAt": "2026-07-30T11:30:00Z"
                              }
                            ],
                            "hypotheses": [
                              {
                                "rank": 1,
                                "possibleCause": "Gateway bağlantı havuzunda kapasite veya connection release problemi",
                                "probability": "HIGH",
                                "supportingEvidenceIds": ["ev-provider-rate", "ev-connections", "ev-prior-incident"],
                                "verificationSteps": [
                                  "Gateway v2.4 connection pool metriklerini incele",
                                  "Önceki sürümle connection lifecycle değişikliklerini karşılaştır"
                                ]
                              }
                            ],
                            "recommendedActions": [
                              {
                                "actionType": "MANUAL_CHECK",
                                "description": "Operatör B bağlantı havuzunu incele",
                                "risk": "LOW",
                                "requiresApproval": false
                              },
                              {
                                "actionType": "CHANGE_PROPOSAL",
                                "description": "Rollback seçeneğini change-management sürecine sun",
                                "risk": "HIGH",
                                "requiresApproval": true
                              }
                            ],
                            "knowledgeReferences": [
                              {
                                "documentId": "INC-2026-041",
                                "version": "1",
                                "chunkId": "chunk-2",
                                "title": "Operatör timeout ve connection pool olayı",
                                "similarityScore": 0.86
                              }
                            ],
                            "confidence": 0.87,
                            "approvalRequired": true,
                            "validation": {
                              "status": "PASSED",
                              "warnings": ["Deploy ile hata başlangıcı arasında korelasyon vardır; nedensellik doğrulanmamıştır."]
                            }
                          }""")))
  public ResponseEntity<InvestigationResponseDto> create(
      @org.springframework.web.bind.annotation.RequestBody InvestigationRequestDto request,
      HttpServletRequest httpRequest) {
    TimeWindow window = validator.validate(request);
    String correlationId = (String) httpRequest.getAttribute("correlationId");
    Investigation outcome =
        orchestrator.runInvestigation(request.question(), window, correlationId);
    return ResponseEntity.ok(toDto(outcome));
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "Get an investigation by id",
      description = "Canonical persisted result snapshot'ını döndürür.")
  public ResponseEntity<InvestigationResponseDto> get(@PathVariable String id) {
    Investigation investigation =
        orchestrator
            .findInvestigation(InvestigationId.of(id))
            .orElseThrow(
                () -> new InvestigationNotFoundException("investigation not found: " + id));
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
