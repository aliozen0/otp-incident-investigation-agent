package com.example.otpsentinel.api;

import com.example.otpsentinel.api.dto.ChatMessageRequestDto;
import com.example.otpsentinel.api.dto.ChatMessageResponseDto;
import com.example.otpsentinel.api.dto.InvestigationDtoMapper;
import com.example.otpsentinel.application.ConversationCommand;
import com.example.otpsentinel.application.ConversationOrchestrator;
import com.example.otpsentinel.application.ConversationResult;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat/messages")
public final class ChatMessageController {

  private final ConversationOrchestrator orchestrator;
  private final ChatMessageRequestValidator validator = new ChatMessageRequestValidator();

  public ChatMessageController(ConversationOrchestrator orchestrator) {
    this.orchestrator = orchestrator;
  }

  @PostMapping
  public ResponseEntity<ChatMessageResponseDto> post(
      @RequestBody ChatMessageRequestDto request, HttpServletRequest httpRequest) {
    String correlationId = (String) httpRequest.getAttribute("correlationId");
    ConversationCommand command = validator.validate(request, correlationId);
    ConversationResult result = orchestrator.handle(command);
    return ResponseEntity.ok(
        new ChatMessageResponseDto(
            UUID.randomUUID().toString(),
            command.sessionId(),
            result.responseType().name(),
            result.assistantMessage(),
            new ChatMessageResponseDto.RouteDto(
                result.route().intent().name(), result.route().confidence(), command.modelId()),
            result.suggestions(),
            result.investigation() == null
                ? null
                : InvestigationDtoMapper.toDto(result.investigation())));
  }
}
