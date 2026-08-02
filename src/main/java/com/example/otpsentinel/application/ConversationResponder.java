package com.example.otpsentinel.application;

@FunctionalInterface
public interface ConversationResponder {
  ConversationReply respond(String message, String sessionContext, String modelId, String locale);
}
