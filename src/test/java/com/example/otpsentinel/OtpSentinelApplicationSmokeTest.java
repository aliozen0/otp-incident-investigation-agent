package com.example.otpsentinel;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Boot smoke test for M0 — proves the Spring context starts against a real pgvector-enabled
 * PostgreSQL (via Testcontainers) and Flyway/Actuator wiring is correct.
 */
@Testcontainers
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class OtpSentinelApplicationSmokeTest {

  private static final DockerImageName PGVECTOR_IMAGE =
      DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres");

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>(PGVECTOR_IMAGE)
          .withDatabaseName("otpsentinel")
          .withUsername("otpsentinel")
          .withPassword("otpsentinel");

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @LocalServerPort private int port;

  @Test
  void contextLoadsAndHealthEndpointReportsUp() {
    TestRestTemplate restTemplate = new TestRestTemplate();

    String body =
        restTemplate.getForObject("http://localhost:" + port + "/actuator/health", String.class);

    assertThat(body).contains("\"status\":\"UP\"");
  }
}
