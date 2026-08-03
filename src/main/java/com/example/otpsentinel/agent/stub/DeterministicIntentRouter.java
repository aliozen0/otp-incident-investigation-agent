package com.example.otpsentinel.agent.stub;

import com.example.otpsentinel.application.IntentDecision;
import com.example.otpsentinel.application.IntentRouter;
import com.example.otpsentinel.application.IntentType;
import java.util.Locale;

/** Purpose-specific offline fixture router; production routing remains LLM-owned. */
public final class DeterministicIntentRouter implements IntentRouter {

  @Override
  public IntentDecision route(String message, String sessionContext, String modelId) {
    String normalized = message.toLowerCase(Locale.forLanguageTag("tr-TR"));
    if (normalized.contains("operatör b nasıl")
        && (sessionContext == null || sessionContext.contains("no prior"))) {
      return new IntentDecision(
          IntentType.CLARIFICATION,
          0.67,
          "Operatör B durumunu inceleme isteği",
          "Operatör B için hangi zaman aralığını ve başarı, timeout veya gecikme metriklerinden hangisini inceleyeyim?");
    }
    if (normalized.contains("operatör b nasıl")) {
      return new IntentDecision(
          IntentType.INVESTIGATION,
          0.88,
          "Önceki OTP bağlamında Operatör B sinyallerini yeniden incele",
          null);
    }
    if (normalized.contains("neden")
        || normalized.contains("karşılaştır")
        || normalized.contains("analiz")
        || normalized.contains("incele")
        || normalized.contains("timeout") && normalized.contains("deploy")) {
      return new IntentDecision(
          IntentType.INVESTIGATION, 0.96, "OTP operasyon sinyallerini incele", null);
    }
    return new IntentDecision(IntentType.CHAT, 0.94, "OTP kapsamlı normal sohbet", null);
  }
}
