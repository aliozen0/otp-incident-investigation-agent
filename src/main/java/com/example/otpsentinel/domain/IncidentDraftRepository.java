package com.example.otpsentinel.domain;

import java.util.Optional;

public interface IncidentDraftRepository {

  void save(IncidentDraft draft);

  Optional<IncidentDraft> findById(IncidentDraftId id);

  Optional<IncidentDraft> findByIdempotencyKey(String idempotencyKey);
}
