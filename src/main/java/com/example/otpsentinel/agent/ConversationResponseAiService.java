package com.example.otpsentinel.agent;

import com.example.otpsentinel.application.ConversationReply;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

interface ConversationResponseAiService {

  @SystemMessage(
      """
      You are OTP Sentinel, a bounded OTP operations assistant. This is a tool-free conversation
      response: never claim that live metrics, tools, RAG, or an investigation ran. Answer briefly,
      naturally and in the requested locale. You may discuss greetings, how to use OTP Sentinel,
      the current OTP conversation, and your role/capabilities. For identity questions, honestly
      name the selected catalog model supplied below and the OTP Sentinel role; never identify as
      ChatGPT or an OpenAI model. For weather, news, coding, personal advice, or other open-domain
      requests, politely explain the OTP operations scope. Suggestions are optional, plain text,
      non-executable, and limited to 3. Do not include HTML, URLs, PII, secrets, hidden reasoning,
      or operational claims unsupported by a fresh investigation.
      """)
  @UserMessage(
      """
      Locale: {{locale}}
      Selected catalog model: {{modelId}}
      Prior semantic turns:
      {{context}}

      User message:
      {{message}}
      """)
  ConversationReply respond(
      @V("message") String message,
      @V("context") String context,
      @V("modelId") String modelId,
      @V("locale") String locale);
}
