package com.example.otpsentinel.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/models")
@Tag(name = "Models", description = "Verified NVIDIA NIM chat models available for selection")
public class ModelsController {

  @GetMapping
  public Map<String, Object> listModels() {
    return Map.of(
        "models", ModelCatalog.VERIFIED_MODELS,
        "options", ModelCatalog.VERIFIED_OPTIONS,
        "defaultModelId", ModelCatalog.DEFAULT_MODEL_ID);
  }
}
