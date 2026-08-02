package com.example.otpsentinel.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.otpsentinel.agent.stub.StubChatModel;
import com.example.otpsentinel.agent.stub.StubScript;
import com.example.otpsentinel.agent.stub.StubScriptStep;
import com.example.otpsentinel.application.ConversationReply;
import com.example.otpsentinel.application.IntentDecision;
import com.example.otpsentinel.application.IntentType;
import java.util.List;
import org.junit.jupiter.api.Test;

class LangChain4jToolFreeConversationTest {

  @Test
  void routerRequestContainsNoToolSpecifications() {
    StubChatModel model =
        new StubChatModel(
            new StubScript(
                List.of(
                    StubScriptStep.finalAnswer(
                        """
                        {"intent":"CHAT","confidence":0.96,
                         "normalizedRequest":"greeting","clarificationQuestion":null}
                        """))));

    IntentDecision result = new LangChain4jIntentRouter(id -> model).route("Merhaba", "", "m");

    assertThat(result.intent()).isEqualTo(IntentType.CHAT);
    assertThat(model.lastRequest().toolSpecifications()).isEmpty();
  }

  @Test
  void conversationResponderRequestContainsNoToolSpecificationsAndKnowsSelectedModel() {
    StubChatModel model =
        new StubChatModel(
            new StubScript(
                List.of(
                    StubScriptStep.finalAnswer(
                        """
                        {"message":"OTP Sentinel, m modeliyle çalışıyorum.","suggestions":[]}
                        """))));

    ConversationReply reply =
        new LangChain4jConversationResponder(id -> model).respond("Hangi modelisin?", "", "m", "tr-TR");

    assertThat(reply.message()).contains("m");
    assertThat(model.lastRequest().toolSpecifications()).isEmpty();
  }
}
