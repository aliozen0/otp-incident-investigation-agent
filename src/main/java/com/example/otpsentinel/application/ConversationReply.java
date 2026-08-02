package com.example.otpsentinel.application;

import java.util.List;

public record ConversationReply(String message, List<String> suggestions) {
  public ConversationReply {
    suggestions = suggestions == null ? List.of() : List.copyOf(suggestions);
  }
}
