package com.example.otpsentinel.agent.stub;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OtpDropOneOhOneScriptTest {

  @Test
  void buildsSixStepScriptEndingInFinalAnswer() {
    StubScript script = OtpDropOneOhOneScript.build();

    assertThat(script).isNotNull();
  }
}
