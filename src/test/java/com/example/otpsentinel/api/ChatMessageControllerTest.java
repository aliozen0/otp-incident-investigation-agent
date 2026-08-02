package com.example.otpsentinel.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.otpsentinel.adapters.persistence.AbstractPostgresIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatMessageControllerTest extends AbstractPostgresIntegrationTest {

  private static final String MODEL = "meta/llama-3.1-8b-instruct";
  @Autowired private TestRestTemplate restTemplate;

  @Test
  void greetingAndIdentityAreChatWithoutInvestigationPersistence() {
    String session = UUID.randomUUID().toString();

    ResponseEntity<String> greeting = post("Merhaba", session, "AUTO", "THOROUGH");
    ResponseEntity<String> identity = post("Hangi modelisin?", session, "AUTO", "THOROUGH");

    assertThat(greeting.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(greeting.getBody())
        .contains("\"responseType\":\"CHAT\"")
        .contains("\"investigation\":null");
    assertThat(identity.getBody()).contains(MODEL).contains("\"responseType\":\"CHAT\"");
    ResponseEntity<String> history =
        restTemplate.getForEntity("/api/v1/sessions/" + session + "/investigations", String.class);
    assertThat(history.getBody()).isEqualTo("[]");
  }

  @Test
  void ambiguousMessageClarifiesWithoutInvestigation() {
    String session = UUID.randomUUID().toString();

    ResponseEntity<String> response = post("Operatör B nasıl?", session, "AUTO", "THOROUGH");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody())
        .contains("\"responseType\":\"CLARIFICATION\"")
        .contains("hangi zaman aralığını")
        .contains("\"investigation\":null");
  }

  @Test
  void explicitInvestigationUsesExistingPipelineAndReturnsCanonicalVisualization() {
    String session = UUID.randomUUID().toString();

    ResponseEntity<String> response =
        post(
            "Son 15 dakikada OTP başarı oranı neden düştü?",
            session,
            "INVESTIGATION",
            "THOROUGH");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody())
        .contains("\"responseType\":\"INVESTIGATION\"")
        .contains("\"status\":\"ANOMALY_CONFIRMED\"")
        .contains("\"id\":\"success-comparison\"")
        .contains("ev-otp-success-rate-current");
  }

  @Test
  void rejectsUnknownModelAndMalformedSessionWithProblemDetails() {
    String body =
        """
        {"message":"Merhaba","sessionId":"not-a-uuid","modelId":"unknown/model",
         "interactionMode":"AUTO","investigationMode":"THOROUGH","locale":"tr-TR"}
        """;
    ResponseEntity<String> response = postBody(body);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("INVALID_CHAT_REQUEST");
  }

  private ResponseEntity<String> post(
      String message, String session, String interactionMode, String investigationMode) {
    String body =
        """
        {"message":"%s","sessionId":"%s","modelId":"%s",
         "interactionMode":"%s","investigationMode":"%s","locale":"tr-TR"}
        """
            .formatted(message, session, MODEL, interactionMode, investigationMode);
    return postBody(body);
  }

  private ResponseEntity<String> postBody(String body) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return restTemplate.postForEntity(
        "/api/v1/chat/messages", new HttpEntity<>(body, headers), String.class);
  }
}
