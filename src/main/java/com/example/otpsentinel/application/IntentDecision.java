package com.example.otpsentinel.application;

public record IntentDecision(
    IntentType intent,
    double confidence,
    String normalizedRequest,
    String clarificationQuestion) {}
