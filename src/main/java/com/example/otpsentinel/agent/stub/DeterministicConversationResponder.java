package com.example.otpsentinel.agent.stub;

import com.example.otpsentinel.application.ConversationReply;
import com.example.otpsentinel.application.ConversationResponder;
import java.util.List;
import java.util.Locale;

/** Offline, request-independent responder fixtures for CI and the default demo. */
public final class DeterministicConversationResponder implements ConversationResponder {

  @Override
  public ConversationReply respond(
      String message, String sessionContext, String modelId, String locale) {
    String normalized = message.toLowerCase(Locale.forLanguageTag("tr-TR"));
    if (normalized.contains("model")) {
      return new ConversationReply(
          "OTP Sentinel operasyon asistanıyım; bu yanıtta seçili "
              + modelId
              + " modelini kullanıyorum. İnceleme istediğinizde yalnız onaylı salt-okunur araçlarla kanıt toplarım.",
          List.of("Neleri inceleyebilirsin?"));
    }
    if (normalized.contains("hava") || normalized.contains("haber") || normalized.contains("kod")) {
      return new ConversationReply(
          "Ben sınırlı kapsamlı OTP Sentinel operasyon asistanıyım. Bu talep kapsamım dışında; OTP başarı, timeout, kuyruk, operatör veya değişiklik sinyallerini konuşabiliriz.",
          List.of("Son 15 dakikadaki OTP başarısını incele"));
    }
    return new ConversationReply(
        "Merhaba! OTP Sentinel olarak OTP operasyon sorularını yanıtlayabilir, belirsiz isteği netleştirebilir veya kanıta dayalı bir inceleme başlatabilirim.",
        List.of("Neleri inceleyebilirsin?", "Son 15 dakikadaki düşüşü analiz et"));
  }
}
