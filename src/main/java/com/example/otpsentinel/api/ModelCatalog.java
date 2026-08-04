package com.example.otpsentinel.api;

import java.util.List;

/**
 * Chat model ids exposed to the M12 console's model picker. Only models with a passing
 * {@code @Tag("local-live")} compatibility spike (docs/19-technology-baseline.md) are listed here —
 * this is intentionally a small static allowlist, not a live catalog query.
 */
public final class ModelCatalog {

  public record ModelOption(
      String id,
      String label,
      String provider,
      String profile,
      String description,
      boolean verified) {}

  public static final List<ModelOption> VERIFIED_OPTIONS =
      List.of(
          new ModelOption(
              "meta/llama-3.1-8b-instruct",
              "Llama 3.1 8B",
              "Meta / NVIDIA NIM",
              "FAST",
              "Hızlı operasyon sorguları ve düşük gecikmeli incelemeler",
              true),
          new ModelOption(
              "meta/llama-3.3-70b-instruct",
              "Llama 3.3 70B",
              "Meta / NVIDIA NIM",
              "BALANCED",
              "Daha kapsamlı hipotez üretimi ve güçlü tool calling",
              true),
          new ModelOption(
              "nvidia/llama-3.3-nemotron-super-49b-v1.5",
              "Llama 3.3 Nemotron Super 49B",
              "NVIDIA NIM",
              "DEEP_ANALYSIS",
              "Karmaşık olaylarda ayrıntılı muhakeme ve güvenilir araç kullanımı",
              true),
          new ModelOption(
              "nvidia/nvidia-nemotron-nano-9b-v2",
              "Nemotron Nano 9B v2",
              "NVIDIA NIM",
              "FAST",
              "Küçük ve hızlı; sıralı iki araç ve structured output canlı testte doğrulandı",
              true),
          new ModelOption(
              "nvidia/nemotron-3-nano-30b-a3b",
              "Nemotron 3 Nano 30B",
              "NVIDIA NIM",
              "EFFICIENT",
              "Hız ve analiz kalitesi arasında dengeli, verimli inceleme",
              true),
          new ModelOption(
              "nvidia/nemotron-3-super-120b-a12b",
              "Nemotron 3 Super 120B",
              "NVIDIA NIM",
              "ADVANCED",
              "Sıralı çoklu araç ve structured output doğrulanmış gelişmiş analiz modeli",
              true),
          new ModelOption(
              "nvidia/nemotron-3-ultra-550b-a55b",
              "Nemotron 3 Ultra 550B",
              "NVIDIA NIM",
              "FRONTIER",
              "Karmaşık agentic incelemeler için doğrulanmış frontier model",
              true));

  public static final List<String> VERIFIED_MODELS =
      VERIFIED_OPTIONS.stream().map(ModelOption::id).toList();

  /**
   * Nemotron Nano 9B by measurement, not by size: over repeated live runs against the seeded
   * telemetry it completes the full tool sequence and confirms the anomaly, where the 8B model
   * frequently stops early and reports "no anomaly". The 70B+ models answer well but take ~30 s per
   * tool call on the shared NIM endpoint and regularly return 503, which times an investigation out.
   */
  public static final String DEFAULT_MODEL_ID = "nvidia/nvidia-nemotron-nano-9b-v2";

  private ModelCatalog() {}
}
