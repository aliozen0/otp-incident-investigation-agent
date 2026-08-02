package com.example.otpsentinel.agent;

import java.util.List;

/** Model-facing loose-enum proposal; deterministic validation maps it to a canonical spec. */
public record VisualizationProposal(
    String id,
    String type,
    String title,
    String xAxisLabel,
    String yAxisLabel,
    String unit,
    List<Series> series,
    List<Point> points) {

  public record Series(String key, String label) {}

  public record Point(String label, String seriesKey, double value, String evidenceId) {}
}
