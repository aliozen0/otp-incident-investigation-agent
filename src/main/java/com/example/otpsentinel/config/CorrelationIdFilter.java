package com.example.otpsentinel.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String correlationId = request.getHeader("X-Correlation-Id");
    if (correlationId == null || correlationId.isBlank()) {
      correlationId = "corr-" + UUID.randomUUID();
    }
    request.setAttribute("correlationId", correlationId);
    response.setHeader("X-Correlation-Id", correlationId);
    response.setHeader("X-Demo-Mode", "true");
    chain.doFilter(request, response);
  }
}
