package com.example.otpsentinel.application;

import java.util.Optional;

/**
 * Turns validated findings into the sentence shown in the chat bubble. Returning an empty result
 * means "keep the model's own summary" — narration is a readability layer, never a requirement.
 */
public interface InvestigationNarrator {

  Optional<String> narrate(String findings);

  /** Offline/stub default: no rewriting at all. */
  InvestigationNarrator NONE = findings -> Optional.empty();
}
