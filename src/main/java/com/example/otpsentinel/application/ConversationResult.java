package com.example.otpsentinel.application;

import com.example.otpsentinel.domain.Investigation;
import java.util.List;

public record ConversationResult(
    IntentType responseType,
    String assistantMessage,
    IntentDecision route,
    List<String> suggestions,
    Investigation investigation) {

  public ConversationResult {
    suggestions = List.copyOf(suggestions);
  }
}
