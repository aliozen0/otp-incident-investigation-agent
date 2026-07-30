package com.example.otpsentinel.adapters.persistence;

import com.example.otpsentinel.domain.Evidence;
import com.example.otpsentinel.domain.Hypothesis;
import com.example.otpsentinel.domain.Investigation;
import com.example.otpsentinel.domain.InvestigationId;
import com.example.otpsentinel.domain.InvestigationPhase;
import com.example.otpsentinel.domain.InvestigationRepository;
import com.example.otpsentinel.domain.InvestigationStatus;
import com.example.otpsentinel.domain.RecommendedAction;
import com.example.otpsentinel.domain.Severity;
import com.example.otpsentinel.domain.TimeWindow;
import com.example.otpsentinel.domain.ValidationReport;
import com.fasterxml.jackson.core.type.TypeReference;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * {@link InvestigationRepository} on plain JdbcTemplate; nested collections are stored as JSONB.
 */
public final class JdbcInvestigationRepository implements InvestigationRepository {

  private static final String UPSERT =
      """
      INSERT INTO investigation (
        id, question, time_window_start, time_window_end, prompt_version, schema_version,
        phase, result_status, severity, confidence, validation_report,
        evidence, hypotheses, recommended_actions, knowledge_references, tool_executions,
        updated_at
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())
      ON CONFLICT (id) DO UPDATE SET
        phase = EXCLUDED.phase,
        result_status = EXCLUDED.result_status,
        severity = EXCLUDED.severity,
        confidence = EXCLUDED.confidence,
        validation_report = EXCLUDED.validation_report,
        evidence = EXCLUDED.evidence,
        hypotheses = EXCLUDED.hypotheses,
        recommended_actions = EXCLUDED.recommended_actions,
        knowledge_references = EXCLUDED.knowledge_references,
        tool_executions = EXCLUDED.tool_executions,
        updated_at = now()
      """;

  private static final String FIND_BY_ID = "SELECT * FROM investigation WHERE id = ?";

  private final JdbcTemplate jdbcTemplate;

  public JdbcInvestigationRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
  }

  @Override
  public void save(Investigation investigation) {
    jdbcTemplate.update(
        UPSERT,
        investigation.id().value(),
        investigation.question(),
        Timestamp.from(investigation.resolvedTimeWindow().startAt()),
        Timestamp.from(investigation.resolvedTimeWindow().endAt()),
        investigation.promptVersion(),
        investigation.schemaVersion(),
        investigation.phase().name(),
        investigation.resultStatus() == null ? null : investigation.resultStatus().name(),
        investigation.severity() == null ? null : investigation.severity().name(),
        investigation.confidence(),
        JsonColumnMapper.toJsonb(investigation.validationReport()),
        JsonColumnMapper.toJsonb(investigation.evidence()),
        JsonColumnMapper.toJsonb(investigation.hypotheses()),
        JsonColumnMapper.toJsonb(investigation.recommendedActions()),
        JsonColumnMapper.toJsonb(investigation.knowledgeReferences()),
        JsonColumnMapper.toJsonb(investigation.toolExecutions()));
  }

  @Override
  public Optional<Investigation> findById(InvestigationId id) {
    return jdbcTemplate.query(FIND_BY_ID, this::mapRow, id.value()).stream().findFirst();
  }

  private Investigation mapRow(ResultSet rs, int rowNum) throws SQLException {
    return Investigation.reconstitute(
        new InvestigationId(rs.getObject("id", UUID.class)),
        rs.getString("question"),
        new TimeWindow(
            rs.getTimestamp("time_window_start").toInstant(),
            rs.getTimestamp("time_window_end").toInstant()),
        rs.getString("prompt_version"),
        rs.getString("schema_version"),
        InvestigationPhase.valueOf(rs.getString("phase")),
        nullableEnum(rs.getString("result_status"), InvestigationStatus::valueOf),
        nullableEnum(rs.getString("severity"), Severity::valueOf),
        readList(rs, "evidence", new TypeReference<List<Evidence>>() {}),
        readList(rs, "hypotheses", new TypeReference<List<Hypothesis>>() {}),
        readList(rs, "recommended_actions", new TypeReference<List<RecommendedAction>>() {}),
        readList(rs, "knowledge_references", new TypeReference<List<String>>() {}),
        (Double) rs.getObject("confidence"),
        readNullable(rs, "validation_report", new TypeReference<ValidationReport>() {}),
        readList(rs, "tool_executions", new TypeReference<List<String>>() {}));
  }

  private <T> T nullableEnum(String value, java.util.function.Function<String, T> parse) {
    return value == null ? null : parse.apply(value);
  }

  private <T> List<T> readList(ResultSet rs, String column, TypeReference<List<T>> type)
      throws SQLException {
    String json = rs.getString(column);
    return json == null ? List.of() : JsonColumnMapper.fromJson(json, type);
  }

  private <T> T readNullable(ResultSet rs, String column, TypeReference<T> type)
      throws SQLException {
    String json = rs.getString(column);
    return json == null ? null : JsonColumnMapper.fromJson(json, type);
  }
}
