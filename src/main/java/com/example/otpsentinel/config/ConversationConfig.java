package com.example.otpsentinel.config;

import com.example.otpsentinel.agent.LangChain4jConversationResponder;
import com.example.otpsentinel.agent.LangChain4jIntentRouter;
import com.example.otpsentinel.agent.stub.DeterministicConversationResponder;
import com.example.otpsentinel.agent.stub.DeterministicIntentRouter;
import com.example.otpsentinel.application.ConversationOrchestrator;
import com.example.otpsentinel.application.ConversationResponder;
import com.example.otpsentinel.application.IntentRouter;
import com.example.otpsentinel.application.InvestigationExecutor;
import com.example.otpsentinel.application.InvestigationTimeWindowResolver;
import com.example.otpsentinel.application.SemanticSessionContextStore;
import dev.langchain4j.model.chat.ChatModel;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConversationConfig {

  @Bean
  IntentRouter intentRouter(
      @Value("${AI_MODE:stub}") String aiMode, Function<String, ChatModel> chatModelFactory) {
    return "live".equalsIgnoreCase(aiMode)
        ? new LangChain4jIntentRouter(chatModelFactory)
        : new DeterministicIntentRouter();
  }

  @Bean
  ConversationResponder conversationResponder(
      @Value("${AI_MODE:stub}") String aiMode, Function<String, ChatModel> chatModelFactory) {
    return "live".equalsIgnoreCase(aiMode)
        ? new LangChain4jConversationResponder(chatModelFactory)
        : new DeterministicConversationResponder();
  }

  @Bean
  ConversationOrchestrator conversationOrchestrator(
      IntentRouter intentRouter,
      ConversationResponder conversationResponder,
      InvestigationOrchestrator investigationOrchestrator,
      @Value("${otp-sentinel.ai.max-repair-attempts:1}") int maxRepairAttempts,
      @Value("${otp-sentinel.ai.semantic-context-max-turns:12}") int maxTurns,
      @Value("${otp-sentinel.ai.chat-memory-max-sessions:1000}") int maxSessions) {
    InvestigationTimeWindowResolver timeWindowResolver = new InvestigationTimeWindowResolver();
    InvestigationExecutor executor =
        command ->
            investigationOrchestrator.runInvestigation(
                command.message(),
                timeWindowResolver.resolve(
                    command.message(), command.explicitStartAt(), command.explicitEndAt()),
                command.correlationId(),
                command.sessionId(),
                command.modelId(),
                command.investigationMode());
    return new ConversationOrchestrator(
        intentRouter,
        conversationResponder,
        executor,
        new SemanticSessionContextStore(maxTurns, maxSessions),
        maxRepairAttempts);
  }
}
