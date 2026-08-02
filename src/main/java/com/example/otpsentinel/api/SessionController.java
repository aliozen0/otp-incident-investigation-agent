package com.example.otpsentinel.api;

import com.example.otpsentinel.api.dto.InvestigationDtoMapper;
import com.example.otpsentinel.api.dto.InvestigationResponseDto;
import com.example.otpsentinel.config.InvestigationOrchestrator;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sessions")
@Tag(name = "Sessions", description = "Chat-thread history for the M12 console sidebar")
public class SessionController {

  private final InvestigationOrchestrator orchestrator;

  public SessionController(InvestigationOrchestrator orchestrator) {
    this.orchestrator = orchestrator;
  }

  @GetMapping("/{sessionId}/investigations")
  public List<InvestigationResponseDto> investigationsForSession(@PathVariable String sessionId) {
    return orchestrator.findBySessionId(sessionId).stream()
        .map(InvestigationDtoMapper::toDto)
        .toList();
  }
}
