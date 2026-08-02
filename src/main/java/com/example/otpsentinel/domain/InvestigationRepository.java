package com.example.otpsentinel.domain;

import java.util.List;
import java.util.Optional;

public interface InvestigationRepository {

  void save(Investigation investigation);

  Optional<Investigation> findById(InvestigationId id);

  List<Investigation> findBySessionId(String sessionId);
}
