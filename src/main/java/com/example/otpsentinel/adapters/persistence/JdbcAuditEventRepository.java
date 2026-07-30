package com.example.otpsentinel.adapters.persistence;

import com.example.otpsentinel.domain.AuditEvent;
import com.example.otpsentinel.domain.AuditEventRepository;
import com.example.otpsentinel.domain.AuditEventType;
import com.example.otpsentinel.domain.IncidentDraftId;
import com.example.otpsentinel.domain.InvestigationId;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/** {@link AuditEventRepository} on plain JdbcTemplate; only INSERT/SELECT, no UPDATE/DELETE. */
public final class JdbcAuditEventRepository implements AuditEventRepository {

  private static final String INSERT =
      """
      INSERT INTO audit_event (
        id, occurred_at, actor, action, investigation_id, approval_id, correlation_id,
        result, policy_version
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
      """;

  private static final String FIND_BY_INVESTIGATION_ID =
      "SELECT * FROM audit_event WHERE investigation_id = ? ORDER BY occurred_at ASC";

  private final JdbcTemplate jdbcTemplate;

  public JdbcAuditEventRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
  }

  @Override
  public void append(AuditEvent event) {
    jdbcTemplate.update(
        INSERT,
        event.id(),
        Timestamp.from(event.occurredAt()),
        event.actor(),
        event.action().name(),
        event.investigationId() == null ? null : event.investigationId().value(),
        event.approvalId() == null ? null : event.approvalId().value(),
        event.correlationId(),
        event.result(),
        event.policyVersion());
  }

  @Override
  public List<AuditEvent> findByInvestigationId(InvestigationId investigationId) {
    return jdbcTemplate.query(FIND_BY_INVESTIGATION_ID, this::mapRow, investigationId.value());
  }

  private AuditEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
    UUID investigationId = rs.getObject("investigation_id", UUID.class);
    UUID approvalId = rs.getObject("approval_id", UUID.class);
    return new AuditEvent(
        rs.getObject("id", UUID.class),
        rs.getTimestamp("occurred_at").toInstant(),
        rs.getString("actor"),
        AuditEventType.valueOf(rs.getString("action")),
        investigationId == null ? null : new InvestigationId(investigationId),
        approvalId == null ? null : new IncidentDraftId(approvalId),
        rs.getString("correlation_id"),
        rs.getString("result"),
        rs.getString("policy_version"));
  }
}
