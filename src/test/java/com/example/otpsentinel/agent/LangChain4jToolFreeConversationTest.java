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
  void routerPromptDistinguishesFreshAmbiguityFromContextualFollowUp() {
    StubChatModel model =
        new StubChatModel(
            new StubScript(
                List.of(
                    StubScriptStep.finalAnswer(
                        """
                        {"intent":"CLARIFICATION","confidence":0.82,
                         "normalizedRequest":"provider status","clarificationQuestion":"Which signal and time range?"}
                        """))));

    new LangChain4jIntentRouter(id -> model).route("Operatör B nasıl?", "no prior turns", "m");

    assertThat(model.lastRequest().messages().toString())
        .contains("A vague OTP operational status request is not CHAT")
        .contains("With prior semantic turns exactly")
        .contains("Operatör B nasıl?")
        .contains("With prior concrete investigation context");
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
        new LangChain4jConversationResponder(id -> model)
            .respond("Hangi modelisin?", "", "m", "tr-TR");

    assertThat(reply.message()).contains("m");
    assertThat(model.lastRequest().toolSpecifications()).isEmpty();
    assertThat(model.lastRequest().messages().toString())
        .contains("selected catalog model string verbatim")
        .contains("do not paraphrase or translate it");
  }
}
