package com.example.otpsentinel.agent;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Map;

/**
 * Id-only citation of an application-minted evidence id (ADR-008: the model cites, never mints).
 */
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public record EvidenceReference(String evidenceId) {
  public EvidenceReference {
    if (evidenceId == null || evidenceId.isBlank()) {
      throw new IllegalArgumentException("evidenceId must not be blank");
    }
  }

  /**
   * The schema asks for a single-field citation object, but live models just as often emit the bare
   * id string ({@code "evidence": ["ev-1"]}). Both shapes carry exactly the same information, so
   * accepting the string form costs no safety and stops a whole valid analysis from being discarded
   * on a formatting slip. The id is still checked against collected evidence by ClaimValidator.
   */
  @JsonCreator
  public static EvidenceReference of(Object raw) {
    if (raw instanceof String id) {
      return new EvidenceReference(id);
    }
    if (raw instanceof Map<?, ?> fields) {
      Object id = fields.get("evidenceId");
      if (id == null) {
        id = fields.get("id");
      }
      return new EvidenceReference(id == null ? null : id.toString());
    }
    throw new IllegalArgumentException("evidence citation must be an id or an object with one");
  }
}
