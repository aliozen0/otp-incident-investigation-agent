package com.example.otpsentinel.domain;

import java.util.List;
import java.util.Objects;

/** Canonical, renderer-agnostic and non-executable visualization snapshot. */
public record VisualizationSpec(
    String id,
    VisualizationType type,
    String title,
    String xAxisLabel,
    String yAxisLabel,
    VisualizationUnit unit,
    List<Series> series,
    List<Point> points) {

  public record Series(String key, String label) {}

  public record Point(String label, String seriesKey, double value, String evidenceId) {}

  public VisualizationSpec {
    Objects.requireNonNull(type, "type must not be null");
    Objects.requireNonNull(unit, "unit must not be null");
    series = List.copyOf(series);
    points = List.copyOf(points);
  }
}
