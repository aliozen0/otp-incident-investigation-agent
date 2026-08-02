package com.example.otpsentinel.api;

import java.util.List;

/**
 * Chat model ids exposed to the M12 console's model picker. Only models with a passing
 * {@code @Tag("local-live")} compatibility spike (docs/19-technology-baseline.md) are listed here —
 * this is intentionally a small static allowlist, not a live catalog query.
 */
public final class ModelCatalog {

  public static final List<String> VERIFIED_MODELS =
      List.of("meta/llama-3.1-8b-instruct", "meta/llama-3.3-70b-instruct");

  private ModelCatalog() {}
}
