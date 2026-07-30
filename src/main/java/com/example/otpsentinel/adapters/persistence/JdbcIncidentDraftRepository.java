package com.example.otpsentinel.adapters.persistence;

import com.example.otpsentinel.domain.Approval;
import com.example.otpsentinel.domain.IncidentDraft;
import com.example.otpsentinel.domain.IncidentDraftId;
import com.example.otpsentinel.domain.IncidentDraftRepository;
import com.example.otpsentinel.domain.IncidentDraftStatus;
import com.example.otpsentinel.domain.InvestigationId;
import com.fasterxml.jackson.core.type.TypeReference;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * {@link IncidentDraftRepository} on plain JdbcTemplate. Idempotency (AC-014/FR-015/SEC-006) is
 * enforced by the {@code uq_incident_draft_idempotency_key} unique constraint (V2 migration): a
 * second aggregate created with an already-used key fails the INSERT with a data integrity
 * violation instead of silently producing a second row.
 */
public final class JdbcIncidentDraftRepository implements IncidentDraftRepository {

  private static final String UPSERT =
      """
      INSERT INTO incident_draft (
        id, investigation_id, payload, idempotency_key, status, approval, external_incident_id,
        updated_at
      ) VALUES (?, ?, ?, ?, ?, ?, ?, now())
      ON CONFLICT (id) DO UPDATE SET
        status = EXCLUDED.status,
        approval = EXCLUDED.approval,
        external_incident_id = EXCLUDED.external_incident_id,
        updated_at = now()
      """;

  private static final String FIND_BY_ID = "SELECT * FROM incident_draft WHERE id = ?";
  private static final String FIND_BY_IDEMPOTENCY_KEY =
      "SELECT * FROM incident_draft WHERE idempotency_key = ?";

  private final JdbcTemplate jdbcTemplate;

  public JdbcIncidentDraftRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
  }

  @Override
  public void save(IncidentDraft draft) {
    jdbcTemplate.update(
        UPSERT,
        draft.id().value(),
        draft.investigationId().value(),
        draft.payload(),
        draft.idempotencyKey(),
        draft.status().name(),
        draft.approval() == null ? null : JsonColumnMapper.toJsonb(draft.approval()),
        draft.externalIncidentId());
  }

  @Override
  public Optional<IncidentDraft> findById(IncidentDraftId id) {
    return jdbcTemplate.query(FIND_BY_ID, this::mapRow, id.value()).stream().findFirst();
  }

  @Override
  public Optional<IncidentDraft> findByIdempotencyKey(String idempotencyKey) {
    return jdbcTemplate.query(FIND_BY_IDEMPOTENCY_KEY, this::mapRow, idempotencyKey).stream()
        .findFirst();
  }

  private IncidentDraft mapRow(ResultSet rs, int rowNum) throws SQLException {
    String approvalJson = rs.getString("approval");
    return IncidentDraft.reconstitute(
        new IncidentDraftId(rs.getObject("id", UUID.class)),
        new InvestigationId(rs.getObject("investigation_id", UUID.class)),
        rs.getString("payload"),
        rs.getString("idempotency_key"),
        IncidentDraftStatus.valueOf(rs.getString("status")),
        approvalJson == null
            ? null
            : JsonColumnMapper.fromJson(approvalJson, new TypeReference<Approval>() {}),
        rs.getString("external_incident_id"));
  }
}
