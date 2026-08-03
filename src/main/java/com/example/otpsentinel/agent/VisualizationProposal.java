package com.example.otpsentinel.agent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Model-facing loose-enum proposal; deterministic validation maps it to a canonical spec. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record VisualizationProposal(
    String id,
    String type,
    String title,
    String xAxisLabel,
    String yAxisLabel,
    String unit,
    List<Series> series,
    List<Point> points) {

  /**
   * Live models routinely nest the point list inside each series instead of keeping it flat, and
   * with strict binding that shape reaches the validator as "no points" — or fails the whole
   * analysis on an unknown property. Both layouts carry identical data, so they are normalized here
   * and the deterministic {@code VisualizationValidator} still decides what may be rendered.
   */
  @JsonCreator
  public static VisualizationProposal of(
      @JsonProperty("id") String id,
      @JsonProperty("type") String type,
      @JsonProperty("title") String title,
      @JsonProperty("xAxisLabel") String xAxisLabel,
      @JsonProperty("yAxisLabel") String yAxisLabel,
      @JsonProperty("unit") String unit,
      @JsonProperty("series") List<Map<String, Object>> series,
      @JsonProperty("points") List<Map<String, Object>> points) {
    List<Series> parsedSeries = new ArrayList<>();
    List<Point> parsedPoints = new ArrayList<>();
    if (series != null) {
      for (Map<String, Object> item : series) {
        if (item == null) {
          continue;
        }
        parsedSeries.add(new Series(text(item.get("key")), text(item.get("label"))));
        parsedPoints.addAll(readPoints(item.get("points")));
      }
    }
    if (points != null) {
      parsedPoints.addAll(readPoints(points));
    }
    return new VisualizationProposal(
        id, type, title, xAxisLabel, yAxisLabel, unit, parsedSeries, parsedPoints);
  }

  private static List<Point> readPoints(Object raw) {
    if (!(raw instanceof List<?> items)) {
      return List.of();
    }
    List<Point> parsed = new ArrayList<>();
    for (Object item : items) {
      if (item instanceof Map<?, ?> fields) {
        parsed.add(
            Point.of(
                text(fields.get("label")),
                text(fields.get("seriesKey")),
                fields.get("value"),
                text(fields.get("evidenceId"))));
      }
    }
    return parsed;
  }

  private static String text(Object value) {
    return value == null ? null : value.toString();
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Series(String key, String label) {}

  /**
   * {@code value} is boxed and parsed leniently on purpose: a live model regularly emits a
   * descriptive string (e.g. a TABLE cell) where the schema asks for a number, and with a strict
   * {@code double} component that single cell makes Jackson reject the whole analysis — losing a
   * complete, otherwise valid investigation. A non-numeric value becomes {@code null} here and the
   * visualization is rejected by {@code VisualizationValidator}, never silently rendered.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Point(String label, String seriesKey, Double value, String evidenceId) {

    @JsonCreator
    public static Point of(
        @JsonProperty("label") String label,
        @JsonProperty("seriesKey") String seriesKey,
        @JsonProperty("value") Object value,
        @JsonProperty("evidenceId") String evidenceId) {
      return new Point(label, seriesKey, numeric(value), evidenceId);
    }

    private static Double numeric(Object raw) {
      if (raw instanceof Number number) {
        return number.doubleValue();
      }
      if (raw instanceof String text) {
        try {
          return Double.valueOf(text.trim());
        } catch (NumberFormatException notANumber) {
          return null;
        }
      }
      return null;
    }
  }
}
