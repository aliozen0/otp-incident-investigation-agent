package com.example.otpsentinel.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Rewrites a finished investigation into the sentence the operator actually reads.
 *
 * <p>The analysis model is chosen for tool discipline and schema fidelity, not for prose: a small
 * reasoning model produces correct numbers with broken Turkish. This call adds no facts — it is
 * given the validated status, severity, confidence and hypotheses and may only restate them — so
 * the chat bubble stays readable while the evidence trail keeps the model's own words.
 */
interface InvestigationNarratorAiService {

  @SystemMessage(
      """
      You turn a finished OTP incident analysis into two or three plain Turkish sentences for an
      on-call engineer.

      Absolute rules:
      - Use ONLY the findings given below. Never add a number, provider, cause or recommendation
        that is not in them, and never soften or upgrade the reported severity.
      - Write fluent, ordinary Turkish. Real Turkish words only: no invented terms, no foreign
        filler, no mixed-language phrases. Keep provider names, metric names, error codes and ids
        exactly as written.
      - Say what changed, where it concentrated, and what the top hypothesis is. Mention the
        confidence only if it is below 0.6, as an explicit uncertainty.
      - No bullet lists, no headings, no markdown, no quotes around the whole answer, no preamble
        like "Özet:". Just the sentences.
      - If the findings say the analysis is partial or failed, say that plainly instead of implying
        a confirmed diagnosis.
      """)
  @UserMessage(
      """
      Bulgular:
      {{findings}}
      """)
  String narrate(@V("findings") String findings);
}
