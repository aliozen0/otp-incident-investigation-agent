package com.example.otpsentinel.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class IncidentDraftTest {

  private IncidentDraft previewed() {
    return IncidentDraft.preview(
        InvestigationId.generate(), "{\"severity\":\"HIGH\"}", "idem-key-1");
  }

  @Test
  void startsInPreviewedStatus() {
    assertThat(previewed().status()).isEqualTo(IncidentDraftStatus.PREVIEWED);
  }

  // Invariant 7: no incident is created without approval.
  @Test
  void rejectsCreateWithoutApproval() {
    IncidentDraft draft = previewed();

    assertThatThrownBy(() -> draft.create("INC-1")).isInstanceOf(IllegalStateException.class);
    assertThat(draft.externalIncidentId()).isNull();
  }

  @Test
  void acceptsCreateAfterApproval() {
    IncidentDraft draft = previewed();
    draft.approve("ops-engineer");

    draft.create("INC-1");

    assertThat(draft.status()).isEqualTo(IncidentDraftStatus.CREATED);
    assertThat(draft.externalIncidentId()).isEqualTo("INC-1");
  }

  @Test
  void rejectDoesNotCreateAnIncident() {
    IncidentDraft draft = previewed();

    draft.reject("ops-engineer", "false positive");

    assertThat(draft.status()).isEqualTo(IncidentDraftStatus.REJECTED);
    assertThat(draft.externalIncidentId()).isNull();
  }

  // Invariant 8: the same idempotency key always yields a single incident.
  @Test
  void repeatedCreateWithSameIdempotencyKeyDoesNotProduceASecondIncident() {
    IncidentDraft draft = previewed();
    draft.approve("ops-engineer");
    draft.create("INC-1");

    draft.create("INC-2");

    assertThat(draft.externalIncidentId()).isEqualTo("INC-1");
    assertThat(draft.status()).isEqualTo(IncidentDraftStatus.CREATED);
  }
}
