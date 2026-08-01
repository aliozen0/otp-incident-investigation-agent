package com.example.otpsentinel.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.otpsentinel.adapters.persistence.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * ADR-016: CORS must be off by default (same-origin embedded frontend in the demo/production image)
 * and only active under the {@code dev} Spring profile (Vite dev server on a different port during
 * frontend development, M10).
 */
class DevCorsConfigTest {

  @Nested
  @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
  class DefaultProfile extends AbstractPostgresIntegrationTest {
    @Autowired private ApplicationContext context;

    @Test
    void corsConfigBeanIsAbsentByDefault() {
      assertThat(context.getBeanNamesForType(DevCorsConfig.class)).isEmpty();
    }
  }

  @Nested
  @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
  @ActiveProfiles("dev")
  class DevProfile extends AbstractPostgresIntegrationTest {
    @Autowired private ApplicationContext context;

    @Test
    void corsConfigBeanIsPresentUnderDevProfile() {
      assertThat(context.getBeanNamesForType(DevCorsConfig.class)).hasSize(1);
    }
  }
}
