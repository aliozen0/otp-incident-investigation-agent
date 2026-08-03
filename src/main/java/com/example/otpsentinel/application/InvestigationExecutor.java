package com.example.otpsentinel.application;

import com.example.otpsentinel.domain.Investigation;

@FunctionalInterface
public interface InvestigationExecutor {
  Investigation execute(ConversationCommand command);
}
