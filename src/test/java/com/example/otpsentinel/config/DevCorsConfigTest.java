package com.example.otpsentinel.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * ADR-016: CORS must be off by default (same-origin embedded frontend in the demo/production image)
 * and only active under the {@code dev} Spring profile (Vite dev server on a different port during
 * frontend development, M10).
 */
class DevCorsConfigTest {

  @Configuration
  @Import(DevCorsConfig.class)
  static class TestConfig {}

  @Nested
  @SpringBootTest(classes = TestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
  @TestPropertySource(
      properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.datasource.url=jdbc:h2:mem:",
        "spring.h2.console.enabled=false"
      })
  class DefaultProfile {
    @Autowired(required = false)
    private ApplicationContext context;

    @Test
    void corsConfigBeanIsAbsentByDefault() {
      if (context != null) {
        assertThat(context.getBeanNamesForType(DevCorsConfig.class)).isEmpty();
      }
    }
  }

  @Nested
  @SpringBootTest(classes = TestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
  @ActiveProfiles("dev")
  @TestPropertySource(
      properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.datasource.url=jdbc:h2:mem:",
        "spring.h2.console.enabled=false"
      })
  class DevProfile {
    @Autowired private ApplicationContext context;

    @Test
    void corsConfigBeanIsPresentUnderDevProfile() {
      assertThat(context.getBeanNamesForType(DevCorsConfig.class)).hasSize(1);
    }
  }
}
