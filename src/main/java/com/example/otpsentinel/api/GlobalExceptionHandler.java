package com.example.otpsentinel.api;

import com.example.otpsentinel.api.dto.ProblemDetailsDto;
import jakarta.servlet.http.HttpServletRequest;
import java.util.NoSuchElementException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ProblemDetailsDto> handleApiException(ApiException e, HttpServletRequest request) {
    return problem(e.status(), e.title(), e.getMessage(), e.errorCode(), request);
  }

  @ExceptionHandler(NoSuchElementException.class)
  public ResponseEntity<ProblemDetailsDto> handleNotFound(NoSuchElementException e, HttpServletRequest request) {
    return problem(404, "Investigation not found", e.getMessage(), "INVESTIGATION_NOT_FOUND", request);
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ProblemDetailsDto> handleConflict(IllegalStateException e, HttpServletRequest request) {
    return problem(409, "Investigation not ready for decision", e.getMessage(),
        "INVESTIGATION_NOT_ACTIONABLE", request);
  }

  private ResponseEntity<ProblemDetailsDto> problem(
      int status, String title, String detail, String errorCode, HttpServletRequest request) {
    String correlationId = (String) request.getAttribute("correlationId");
    ProblemDetailsDto body = new ProblemDetailsDto(
        "https://errors.example.local/" + errorCode.toLowerCase().replace('_', '-'),
        title, status, detail, request.getRequestURI(), correlationId, errorCode);
    return ResponseEntity.status(status).body(body);
  }
}
