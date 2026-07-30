package com.example.otpsentinel.adapters.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.otpsentinel.domain.AuditEvent;
import com.example.otpsentinel.domain.AuditEventType;
import com.example.otpsentinel.domain.IncidentDraftId;
import com.example.otpsentinel.domain.InvestigationId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

/** FR-017/DATA-005: representative audit events persist, in order, and can never be mutated. */
class JdbcAuditEventRepositoryTest extends AbstractPostgresIntegrationTest {

  @Test
  void appendsAndListsEventsForAnInvestigationInOccurredOrder() {
    InvestigationId investigationId = InvestigationId.generate();
    IncidentDraftId approvalId = IncidentDraftId.generate();
    JdbcAuditEventRepository repository = newAuditEventRepository();

    repository.append(
        AuditEvent.of(
            "operator-1",
            AuditEventType.REQUEST_ACCEPTED,
            investigationId,
            null,
            "corr-1",
            "SUCCESS",
            "policy-v1"));
    repository.append(
        AuditEvent.of(
            "operator-1",
            AuditEventType.TOOL_CALLED,
            investigationId,
            null,
            "corr-1",
            "SUCCESS",
            "policy-v1"));
    repository.append(
        AuditEvent.of(
            "operator-1",
            AuditEventType.APPROVAL_DECIDED,
            investigationId,
            approvalId,
            "corr-1",
            "APPROVE",
            "policy-v1"));
    repository.append(
        AuditEvent.of(
            "operator-1",
            AuditEventType.INCIDENT_CREATED,
            investigationId,
            approvalId,
            "corr-1",
            "SUCCESS",
            "policy-v1"));

    List<AuditEvent> events = repository.findByInvestigationId(investigationId);

    assertThat(events).hasSize(4);
    assertThat(events)
        .extracting(AuditEvent::action)
        .containsExactly(
            AuditEventType.REQUEST_ACCEPTED,
            AuditEventType.TOOL_CALLED,
            AuditEventType.APPROVAL_DECIDED,
            AuditEventType.INCIDENT_CREATED);
    assertThat(events.get(2).approvalId()).isEqualTo(approvalId);
  }

  @Test
  void findByInvestigationIdReturnsEmptyForUnknownInvestigation() {
    List<AuditEvent> events =
        newAuditEventRepository().findByInvestigationId(InvestigationId.generate());

    assertThat(events).isEmpty();
  }

  @Test
  void auditEventIsAppendOnlyAtTheDatabaseLevel() {
    InvestigationId investigationId = InvestigationId.generate();
    JdbcAuditEventRepository repository = newAuditEventRepository();
    AuditEvent event =
        AuditEvent.of(
            "operator-1",
            AuditEventType.REQUEST_ACCEPTED,
            investigationId,
            null,
            "corr-1",
            "SUCCESS",
            "policy-v1");
    repository.append(event);

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    "UPDATE audit_event SET result = ? WHERE id = ?", "TAMPERED", event.id()))
        .isInstanceOf(DataAccessException.class);
    assertThatThrownBy(
            () -> jdbcTemplate.update("DELETE FROM audit_event WHERE id = ?", event.id()))
        .isInstanceOf(DataAccessException.class);

    List<AuditEvent> events = repository.findByInvestigationId(investigationId);
    assertThat(events).hasSize(1);
    assertThat(events.get(0).result()).isEqualTo("SUCCESS");
  }
}
