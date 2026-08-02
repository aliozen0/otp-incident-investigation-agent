package com.example.otpsentinel.agent;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Per-session in-memory {@link ChatMemory}, capped to the last {@code maxMessages} messages
 * (docs/16 ADR-017). Lost on restart by design — investigation results are already persisted
 * separately (M3); this only carries conversational continuity within one session's lifetime.
 *
 * <p>Bounded to {@code maxSessions} entries with LRU eviction: an anonymous investigation (no
 * client {@code sessionId}) uses its own investigation id as memory id and is never revisited, so
 * an unbounded map would leak one {@link ChatMemory} per request for the life of the process.
 */
public final class SessionChatMemoryStore {

  /** Enough sessions for a demo/console instance; eviction only drops the least recently used. */
  public static final int DEFAULT_MAX_SESSIONS = 1000;

  private final int maxMessages;
  private final Map<String, ChatMemory> memories;

  public SessionChatMemoryStore(int maxMessages) {
    this(maxMessages, DEFAULT_MAX_SESSIONS);
  }

  public SessionChatMemoryStore(int maxMessages, int maxSessions) {
    if (maxMessages <= 0) {
      throw new IllegalArgumentException("maxMessages must be positive");
    }
    if (maxSessions <= 0) {
      throw new IllegalArgumentException("maxSessions must be positive");
    }
    this.maxMessages = maxMessages;
    // ponytail: one lock for the whole map; per-session striping only if contention ever shows up.
    this.memories =
        Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, true) {
              @Override
              protected boolean removeEldestEntry(Map.Entry<String, ChatMemory> eldest) {
                return size() > maxSessions;
              }
            });
  }

  public ChatMemory get(String memoryId) {
    Objects.requireNonNull(memoryId, "memoryId must not be null");
    return memories.computeIfAbsent(
        memoryId, id -> MessageWindowChatMemory.withMaxMessages(maxMessages));
  }

  /** Sessions currently held in memory; drops to {@code maxSessions} as older ones are evicted. */
  public int sessionCount() {
    return memories.size();
  }

  /** True when {@code memoryId} still has a live {@link ChatMemory} (i.e. was not LRU-evicted). */
  public boolean hasSession(String memoryId) {
    return memories.containsKey(memoryId);
  }
}
