package com.example.otpsentinel.agent;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Per-session in-memory {@link ChatMemory}, capped to the last {@code maxMessages} messages
 * (docs/16 ADR-017). Lost on restart by design — investigation results are already persisted
 * separately (M3); this only carries conversational continuity within one session's lifetime.
 */
public final class SessionChatMemoryStore {

  private final int maxMessages;
  private final ConcurrentMap<String, ChatMemory> memories = new ConcurrentHashMap<>();

  public SessionChatMemoryStore(int maxMessages) {
    if (maxMessages <= 0) {
      throw new IllegalArgumentException("maxMessages must be positive");
    }
    this.maxMessages = maxMessages;
  }

  public ChatMemory get(String memoryId) {
    Objects.requireNonNull(memoryId, "memoryId must not be null");
    return memories.computeIfAbsent(
        memoryId, id -> MessageWindowChatMemory.withMaxMessages(maxMessages));
  }
}
