package com.example.otpsentinel.domain;

import java.util.List;

/** Append-only: no update/delete is exposed by design (DATA-005). */
public interface AuditEventRepository {

  void append(AuditEvent event);

  List<AuditEvent> findByInvestigationId(InvestigationId investigationId);
}
