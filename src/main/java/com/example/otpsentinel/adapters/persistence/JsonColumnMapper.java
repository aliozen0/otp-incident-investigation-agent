package com.example.otpsentinel.adapters.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.sql.SQLException;
import org.postgresql.util.PGobject;

/** Shared JSONB (de)serialization for repository adapters; domain records stay Jackson-free. */
final class JsonColumnMapper {

  private static final ObjectMapper MAPPER =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          // records like Evidence expose derived getters (isMetric()) that are not constructor
          // parameters; ignore them on read instead of failing the whole row.
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  private JsonColumnMapper() {}

  static PGobject toJsonb(Object value) {
    try {
      PGobject jsonb = new PGobject();
      jsonb.setType("jsonb");
      jsonb.setValue(MAPPER.writeValueAsString(value));
      return jsonb;
    } catch (SQLException e) {
      throw new IllegalStateException("failed to build jsonb value", e);
    } catch (Exception e) {
      throw new IllegalStateException("failed to serialize value to json", e);
    }
  }

  static <T> T fromJson(String json, TypeReference<T> type) {
    try {
      return MAPPER.readValue(json, type);
    } catch (Exception e) {
      throw new IllegalStateException("failed to deserialize json column", e);
    }
  }
}
