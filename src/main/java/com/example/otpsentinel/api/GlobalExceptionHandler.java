package com.example.otpsentinel.api;

import com.example.otpsentinel.api.dto.ProblemDetailsDto;
import com.example.otpsentinel.application.IntentRoutingFailedException;
import com.example.otpsentinel.config.InvestigationNotActionableException;
import com.example.otpsentinel.config.InvestigationNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ProblemDetailsDto> handleApiException(
      ApiException e, HttpServletRequest request) {
    return problem(e.status(), e.title(), e.getMessage(), e.errorCode(), request);
  }

  @ExceptionHandler(InvestigationNotFoundException.class)
  public ResponseEntity<ProblemDetailsDto> handleNotFound(
      InvestigationNotFoundException e, HttpServletRequest request) {
    return problem(
        404, "Investigation not found", e.getMessage(), "INVESTIGATION_NOT_FOUND", request);
  }

  @ExceptionHandler(InvestigationNotActionableException.class)
  public ResponseEntity<ProblemDetailsDto> handleConflict(
      InvestigationNotActionableException e, HttpServletRequest request) {
    return problem(
        409,
        "Investigation not ready for decision",
        e.getMessage(),
        "INVESTIGATION_NOT_ACTIONABLE",
        request);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ProblemDetailsDto> handleBadRequest(
      IllegalArgumentException e, HttpServletRequest request) {
    return problem(400, "Invalid request", e.getMessage(), "INVALID_REQUEST", request);
  }

  @ExceptionHandler(dev.langchain4j.exception.HttpException.class)
  public ResponseEntity<ProblemDetailsDto> handleModelProviderError(
      dev.langchain4j.exception.HttpException e, HttpServletRequest request) {
    return problem(502, "Model provider error", e.getMessage(), "MODEL_PROVIDER_ERROR", request);
  }

  @ExceptionHandler(dev.langchain4j.exception.RetriableException.class)
  public ResponseEntity<ProblemDetailsDto> handleRetriableProviderError(
      dev.langchain4j.exception.RetriableException e, HttpServletRequest request) {
    return problem(502, "Model provider error", e.getMessage(), "MODEL_PROVIDER_ERROR", request);
  }

  @ExceptionHandler(IntentRoutingFailedException.class)
  public ResponseEntity<ProblemDetailsDto> handleIntentRoutingFailed(
      IntentRoutingFailedException e, HttpServletRequest request) {
    return problem(
        502,
        "Intent routing failed",
        "The selected model did not return a valid intent decision after one repair attempt.",
        "INTENT_ROUTING_FAILED",
        request);
  }

  // Note: langchain4j's own TimeoutException (dev.langchain4j.exception.TimeoutException), not
  // java.util.concurrent.TimeoutException -- verified against langchain4j-http-client-jdk 1.18.1
  // sources: JdkHttpClient.execute() catches java.net.http.HttpTimeoutException and rethrows this
  // type.
  @ExceptionHandler(dev.langchain4j.exception.TimeoutException.class)
  public ResponseEntity<ProblemDetailsDto> handleTimeout(
      dev.langchain4j.exception.TimeoutException e, HttpServletRequest request) {
    return problem(
        504, "Investigation timed out", e.getMessage(), "INVESTIGATION_TIMEOUT", request);
  }

  private ResponseEntity<ProblemDetailsDto> problem(
      int status, String title, String detail, String errorCode, HttpServletRequest request) {
    String correlationId = (String) request.getAttribute("correlationId");
    ProblemDetailsDto body =
        new ProblemDetailsDto(
            "https://errors.example.local/" + errorCode.toLowerCase().replace('_', '-'),
            title,
            status,
            detail,
            request.getRequestURI(),
            correlationId,
            errorCode);
    return ResponseEntity.status(status).body(body);
  }
}
