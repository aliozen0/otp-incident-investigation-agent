package com.example.otpsentinel.adapters.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.otpsentinel.domain.IncidentDraft;
import com.example.otpsentinel.domain.IncidentDraftId;
import com.example.otpsentinel.domain.IncidentDraftStatus;
import com.example.otpsentinel.domain.InvestigationId;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

/** AC-014/FR-015/SEC-006: idempotency key uniqueness is enforced by the database. */
class JdbcIncidentDraftRepositoryTest extends AbstractPostgresIntegrationTest {

  @Test
  void survivesRestartThroughApprovalAndCreation() {
    IncidentDraft draft =
        IncidentDraft.preview(InvestigationId.generate(), "{\"summary\":\"drop\"}", "idem-key-1");
    newIncidentDraftRepository().save(draft);

    draft.approve("operator-1");
    newIncidentDraftRepository().save(draft);

    draft.create("EXT-123");
    newIncidentDraftRepository().save(draft);

    Optional<IncidentDraft> reloaded = newIncidentDraftRepository().findById(draft.id());

    assertThat(reloaded).isPresent();
    IncidentDraft restarted = reloaded.get();
    assertThat(restarted.status()).isEqualTo(IncidentDraftStatus.CREATED);
    assertThat(restarted.externalIncidentId()).isEqualTo("EXT-123");
    assertThat(restarted.approval().actor()).isEqualTo("operator-1");
    assertThat(restarted.idempotencyKey()).isEqualTo("idem-key-1");
  }

  @Test
  void duplicateIdempotencyKeyProducesExactlyOneRow() {
    IncidentDraft first =
        IncidentDraft.preview(InvestigationId.generate(), "{\"summary\":\"first\"}", "shared-key");
    newIncidentDraftRepository().save(first);

    IncidentDraft second =
        IncidentDraft.preview(InvestigationId.generate(), "{\"summary\":\"second\"}", "shared-key");

    assertThatThrownBy(() -> newIncidentDraftRepository().save(second))
        .isInstanceOf(DataIntegrityViolationException.class);

    Optional<IncidentDraft> byKey = newIncidentDraftRepository().findByIdempotencyKey("shared-key");
    assertThat(byKey).isPresent();
    assertThat(byKey.get().id()).isEqualTo(first.id());

    Integer rowCount =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM incident_draft WHERE idempotency_key = ?",
            Integer.class,
            "shared-key");
    assertThat(rowCount).isEqualTo(1);
  }

  @Test
  void findByIdReturnsEmptyForUnknownId() {
    Optional<IncidentDraft> reloaded =
        newIncidentDraftRepository().findById(IncidentDraftId.generate());

    assertThat(reloaded).isEmpty();
  }
}
