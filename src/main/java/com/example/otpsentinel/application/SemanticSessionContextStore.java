package com.example.otpsentinel.application;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Bounded semantic user/assistant turns, deliberately separate from investigation tool memory. */
public final class SemanticSessionContextStore {

  private record Turn(String user, String assistant) {}

  private final int maxTurns;
  private final Map<String, Deque<Turn>> sessions;

  public SemanticSessionContextStore(int maxTurns, int maxSessions) {
    if (maxTurns <= 0 || maxSessions <= 0) {
      throw new IllegalArgumentException("context limits must be positive");
    }
    this.maxTurns = maxTurns;
    this.sessions =
        Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, true) {
              @Override
              protected boolean removeEldestEntry(Map.Entry<String, Deque<Turn>> eldest) {
                return size() > maxSessions;
              }
            });
  }

  public void append(String sessionId, String user, String assistant) {
    Objects.requireNonNull(sessionId, "sessionId must not be null");
    Objects.requireNonNull(user, "user must not be null");
    Objects.requireNonNull(assistant, "assistant must not be null");
    synchronized (sessions) {
      Deque<Turn> turns = sessions.computeIfAbsent(sessionId, ignored -> new ArrayDeque<>());
      turns.addLast(new Turn(user, assistant));
      while (turns.size() > maxTurns) {
        turns.removeFirst();
      }
    }
  }

  public String context(String sessionId) {
    synchronized (sessions) {
      Deque<Turn> turns = sessions.get(sessionId);
      if (turns == null) {
        return "(no prior semantic turns)";
      }
      StringBuilder context = new StringBuilder();
      for (Turn turn : turns) {
        context.append("USER: ").append(turn.user()).append('\n');
        context.append("ASSISTANT: ").append(turn.assistant()).append('\n');
      }
      return context.toString();
    }
  }

  public boolean hasSession(String sessionId) {
    return sessions.containsKey(sessionId);
  }
}
