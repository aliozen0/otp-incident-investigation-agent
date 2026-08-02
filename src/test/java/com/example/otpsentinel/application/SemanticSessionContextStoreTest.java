package com.example.otpsentinel.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SemanticSessionContextStoreTest {

  @Test
  void isolatesSessionsAndBoundsTurns() {
    SemanticSessionContextStore store = new SemanticSessionContextStore(2, 2);
    store.append("a", "u1", "a1");
    store.append("a", "u2", "a2");
    store.append("a", "u3", "a3");
    store.append("b", "ub", "ab");

    assertThat(store.context("a")).doesNotContain("u1").contains("u2", "u3");
    assertThat(store.context("b")).contains("ub").doesNotContain("u2");
  }

  @Test
  void evictsLeastRecentlyUsedSession() {
    SemanticSessionContextStore store = new SemanticSessionContextStore(2, 2);
    store.append("a", "u", "a");
    store.append("b", "u", "b");
    store.context("a");
    store.append("c", "u", "c");

    assertThat(store.hasSession("a")).isTrue();
    assertThat(store.hasSession("b")).isFalse();
    assertThat(store.hasSession("c")).isTrue();
  }
}
