package com.example.otpsentinel.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS only for local frontend development against a separate-port Vite dev server (ADR-016). The
 * demo/production image serves the frontend from the same Spring Boot origin (M10), so this bean is
 * intentionally absent unless {@code SPRING_PROFILES_ACTIVE=dev} — never active in
 * default/demo/production.
 */
@Configuration
@Profile("dev")
public class DevCorsConfig implements WebMvcConfigurer {

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/api/**")
        .allowedOrigins("http://localhost:5173")
        .allowedMethods("GET", "POST")
        .allowedHeaders("*");
  }
}
