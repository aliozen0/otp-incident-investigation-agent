package com.example.otpsentinel.api;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.exception.InternalServerException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerTest {

  @Test
  void mapsRetriableProviderFailureToBadGateway() {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/chat/messages");
    request.setAttribute("correlationId", "corr-provider");

    var response =
        new GlobalExceptionHandler()
            .handleRetriableProviderError(
                new InternalServerException("provider rejected tool-call history"), request);

    assertThat(response.getStatusCode().value()).isEqualTo(502);
    assertThat(response.getBody().errorCode()).isEqualTo("MODEL_PROVIDER_ERROR");
    assertThat(response.getBody().correlationId()).isEqualTo("corr-provider");
  }
}
