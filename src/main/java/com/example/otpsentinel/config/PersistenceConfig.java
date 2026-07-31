package com.example.otpsentinel.config;

import com.example.otpsentinel.adapters.persistence.JdbcAuditEventRepository;
import com.example.otpsentinel.adapters.persistence.JdbcIncidentDraftRepository;
import com.example.otpsentinel.adapters.persistence.JdbcInvestigationRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class PersistenceConfig {

  @Bean
  public JdbcInvestigationRepository investigationRepository(JdbcTemplate jdbcTemplate) {
    return new JdbcInvestigationRepository(jdbcTemplate);
  }

  @Bean
  public JdbcIncidentDraftRepository incidentDraftRepository(JdbcTemplate jdbcTemplate) {
    return new JdbcIncidentDraftRepository(jdbcTemplate);
  }

  @Bean
  public JdbcAuditEventRepository auditEventRepository(JdbcTemplate jdbcTemplate) {
    return new JdbcAuditEventRepository(jdbcTemplate);
  }
}
