package com.example.otpsentinel.adapters.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared Testcontainers PostgreSQL fixture for M3 repository tests (docs/13 "Integration" katmanı).
 * Singleton-container pattern: the field is inherited (shared) by every subclass, so it is started
 * once via a static initializer instead of {@code @Container} — that annotation would make the
 * JUnit5 extension stop the shared container after the first subclass, breaking the remaining ones.
 * The Ryuk sidecar reaps it when the JVM exits. Each test truncates the domain tables first so
 * tests stay independent within the single Flyway-migrated schema.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public abstract class AbstractPostgresIntegrationTest {

  private static final DockerImageName PGVECTOR_IMAGE =
      DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres");

  static final PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>(PGVECTOR_IMAGE)
          .withDatabaseName("otpsentinel")
          .withUsername("otpsentinel")
          .withPassword("otpsentinel");

  static {
    postgres.start();
  }

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired protected JdbcTemplate jdbcTemplate;

  @BeforeEach
  void cleanTables() {
    jdbcTemplate.update(
        "TRUNCATE TABLE audit_event, incident_draft, investigation, knowledge_chunk, knowledge_document");
  }

  /** Simulates an application restart: a fresh adapter instance over the same, still-running DB. */
  protected JdbcInvestigationRepository newInvestigationRepository() {
    return new JdbcInvestigationRepository(jdbcTemplate);
  }

  protected JdbcIncidentDraftRepository newIncidentDraftRepository() {
    return new JdbcIncidentDraftRepository(jdbcTemplate);
  }

  protected JdbcAuditEventRepository newAuditEventRepository() {
    return new JdbcAuditEventRepository(jdbcTemplate);
  }
}
