package com.example.otpsentinel.agent;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import org.junit.jupiter.api.Test;

class SessionChatMemoryStoreTest {

  @Test
  void sameSessionIdReturnsTheSameMemoryAcrossCalls() {
    SessionChatMemoryStore store = new SessionChatMemoryStore(10);

    ChatMemory first = store.get("session-A");
    first.add(UserMessage.from("why did OTP success rate drop?"));
    ChatMemory second = store.get("session-A");

    assertThat(second.messages()).hasSize(1);
    assertThat(((UserMessage) second.messages().get(0)).singleText())
        .isEqualTo("why did OTP success rate drop?");
  }

  @Test
  void differentSessionIdDoesNotSeeAnotherSessionsMessages() {
    SessionChatMemoryStore store = new SessionChatMemoryStore(10);
    store.get("session-A").add(UserMessage.from("why did OTP success rate drop?"));

    ChatMemory sessionB = store.get("session-B");

    assertThat(sessionB.messages()).isEmpty();
  }

  @Test
  void windowCapsAtMaxMessages() {
    SessionChatMemoryStore store = new SessionChatMemoryStore(2);
    ChatMemory memory = store.get("session-C");

    memory.add(UserMessage.from("first"));
    memory.add(UserMessage.from("second"));
    memory.add(UserMessage.from("third"));

    assertThat(memory.messages()).hasSize(2);
  }
}
