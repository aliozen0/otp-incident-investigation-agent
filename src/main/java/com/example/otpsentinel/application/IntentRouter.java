package com.example.otpsentinel.application;

@FunctionalInterface
public interface IntentRouter {
  IntentDecision route(String message, String sessionContext, String modelId);
}
