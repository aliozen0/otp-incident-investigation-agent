package com.example.otpsentinel.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.otpsentinel.adapters.persistence.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ModelsControllerTest extends AbstractPostgresIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void listsOnlyVerifiedModels() {
    ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/models", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    // Both verified models must be exposed — a picker with one option is not a picker (Task 4).
    assertThat(response.getBody())
        .contains("meta/llama-3.1-8b-instruct")
        .contains("meta/llama-3.3-70b-instruct")
        .contains("nvidia/llama-3.3-nemotron-super-49b-v1.5")
        .contains("nvidia/nemotron-3-nano-30b-a3b")
        .contains("\"defaultModelId\":\"meta/llama-3.1-8b-instruct\"")
        .contains("\"options\"")
        .contains("\"label\":\"Llama 3.1 8B\"")
        .contains("\"profile\":\"FAST\"")
        .contains("\"verified\":true");
    assertThat(ModelCatalog.VERIFIED_MODELS).hasSize(4);
    assertThat(ModelCatalog.VERIFIED_OPTIONS)
        .extracting(ModelCatalog.ModelOption::id)
        .containsExactlyElementsOf(ModelCatalog.VERIFIED_MODELS);
  }
}
