package com.example.otpsentinel.application;

import com.example.otpsentinel.agent.VisualizationProposal;
import com.example.otpsentinel.domain.Evidence;
import com.example.otpsentinel.domain.VisualizationSpec;
import com.example.otpsentinel.domain.VisualizationType;
import com.example.otpsentinel.domain.VisualizationUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Converts model proposals to canonical visualizations only when every point is evidence-bound. */
public final class VisualizationValidator {

  private static final int MAX_VISUALIZATIONS = 4;
  private static final int MAX_SERIES = 4;
  private static final int MAX_POINTS = 40;
  private static final double TOLERANCE = 0.011;

  public VisualizationValidationResult validate(
      List<VisualizationProposal> proposals, List<Evidence> evidence) {
    if (proposals == null || proposals.isEmpty()) {
      return new VisualizationValidationResult(List.of(), List.of());
    }
    Map<String, Evidence> known = new HashMap<>();
    evidence.forEach(item -> known.put(item.id(), item));
    List<VisualizationSpec> accepted = new ArrayList<>();
    List<String> warnings = new ArrayList<>();
    if (proposals.size() > MAX_VISUALIZATIONS) {
      warnings.add("VISUALIZATION_REJECTED: at most 4 visualizations are allowed");
    }
    for (VisualizationProposal proposal : proposals.stream().limit(MAX_VISUALIZATIONS).toList()) {
      try {
        accepted.add(validateOne(proposal, known));
      } catch (IllegalArgumentException failure) {
        warnings.add("VISUALIZATION_REJECTED: " + failure.getMessage());
      }
    }
    return new VisualizationValidationResult(accepted, warnings);
  }

  private VisualizationSpec validateOne(
      VisualizationProposal proposal, Map<String, Evidence> evidence) {
    if (proposal == null) {
      throw new IllegalArgumentException("proposal is null");
    }
    VisualizationType type = parseType(proposal.type());
    VisualizationUnit unit = parseUnit(proposal.unit());
    String id = plain(proposal.id(), "id", 64);
    String title = plain(proposal.title(), "title", 120);
    String xAxis = optionalPlain(proposal.xAxisLabel(), "xAxisLabel", 80);
    String yAxis = optionalPlain(proposal.yAxisLabel(), "yAxisLabel", 80);
    if (proposal.series() == null
        || proposal.series().isEmpty()
        || proposal.series().size() > MAX_SERIES) {
      throw new IllegalArgumentException("series count must be 1..4");
    }
    if (proposal.points() == null
        || proposal.points().isEmpty()
        || proposal.points().size() > MAX_POINTS) {
      throw new IllegalArgumentException("point count must be 1..40");
    }
    Set<String> seriesKeys = new HashSet<>();
    List<VisualizationSpec.Series> series =
        proposal.series().stream()
            .map(
                item -> {
                  String key = plain(item.key(), "series key", 48);
                  if (!seriesKeys.add(key)) {
                    throw new IllegalArgumentException("duplicate series key");
                  }
                  return new VisualizationSpec.Series(key, plain(item.label(), "series label", 80));
                })
            .toList();
    List<VisualizationSpec.Point> points = new ArrayList<>();
    for (VisualizationProposal.Point point : proposal.points()) {
      if (!Double.isFinite(point.value())) {
        throw new IllegalArgumentException("point value must be finite");
      }
      if (!seriesKeys.contains(point.seriesKey())) {
        throw new IllegalArgumentException("point references unknown series");
      }
      Evidence source = evidence.get(point.evidenceId());
      if (source == null) {
        throw new IllegalArgumentException("unknown evidence " + point.evidenceId());
      }
      if (source.metricName() == null
          || source.metricValue() == null
          || source.metricUnit() == null) {
        throw new IllegalArgumentException("evidence is not numeric");
      }
      double expected = convert(source.metricValue(), source.metricUnit(), unit);
      if (Math.abs(expected - point.value()) > TOLERANCE) {
        throw new IllegalArgumentException("point value does not match evidence");
      }
      points.add(
          new VisualizationSpec.Point(
              plain(point.label(), "point label", 80),
              point.seriesKey(),
              point.value(),
              point.evidenceId()));
    }
    return new VisualizationSpec(id, type, title, xAxis, yAxis, unit, series, points);
  }

  private static double convert(double value, String sourceUnit, VisualizationUnit targetUnit) {
    String normalized = sourceUnit.toLowerCase(Locale.ROOT);
    if (normalized.equals("percent") && targetUnit == VisualizationUnit.PERCENT
        || normalized.equals("ratio") && targetUnit == VisualizationUnit.RATIO
        || normalized.equals("count") && targetUnit == VisualizationUnit.COUNT
        || normalized.equals("milliseconds") && targetUnit == VisualizationUnit.MILLISECONDS
        || normalized.equals("connections") && targetUnit == VisualizationUnit.CONNECTIONS
        || normalized.equals("none") && targetUnit == VisualizationUnit.NONE) {
      return value;
    }
    if (normalized.equals("ratio") && targetUnit == VisualizationUnit.PERCENT) {
      return value * 100.0;
    }
    if (normalized.equals("percent") && targetUnit == VisualizationUnit.RATIO) {
      return value / 100.0;
    }
    throw new IllegalArgumentException("incompatible evidence unit");
  }

  private static VisualizationType parseType(String value) {
    try {
      return VisualizationType.valueOf(value);
    } catch (RuntimeException failure) {
      throw new IllegalArgumentException("unknown visualization type");
    }
  }

  private static VisualizationUnit parseUnit(String value) {
    try {
      return VisualizationUnit.valueOf(value);
    } catch (RuntimeException failure) {
      throw new IllegalArgumentException("unknown visualization unit");
    }
  }

  private static String optionalPlain(String value, String field, int max) {
    return value == null || value.isBlank() ? null : plain(value, field, max);
  }

  private static String plain(String value, String field, int max) {
    if (value == null || value.isBlank() || value.length() > max) {
      throw new IllegalArgumentException(field + " length is invalid");
    }
    String lower = value.toLowerCase(Locale.ROOT);
    if (value.indexOf('<') >= 0
        || value.indexOf('>') >= 0
        || lower.contains("javascript:")
        || lower.contains("http://")
        || lower.contains("https://")) {
      throw new IllegalArgumentException(field + " must be safe plain text");
    }
    return value.trim();
  }
}
