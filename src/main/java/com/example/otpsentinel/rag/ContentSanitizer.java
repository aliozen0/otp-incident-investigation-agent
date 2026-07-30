package com.example.otpsentinel.rag;

import java.util.regex.Pattern;

/**
 * Prompt-injection defense for retrieved content (docs/08-rag-spec.md "Prompt injection koruması",
 * docs/09-security-governance.md AI-004/AI-005): strips script/HTML and enforces a size limit
 * before anything is chunked or embedded. The document-type allowlist is {@link
 * KnowledgeDocumentType} itself.
 *
 * <p>{@link #hasInstructionPattern(String)} is a coarse heuristic signal only, not enforcement —
 * the real policy check is M6's; sanitized content is always treated as untrusted reference data
 * regardless of whether this pattern fires.
 */
public final class ContentSanitizer {

  static final int MAX_CONTENT_CHARS = 20_000;

  private static final Pattern SCRIPT_TAG =
      Pattern.compile("<script\\b[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
  private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");
  private static final Pattern INSTRUCTION_PATTERN =
      Pattern.compile(
          "ignore(?:\\s+\\w+){0,3}\\s+instructions"
              + "|disregard(?:\\s+\\w+){0,3}\\s+instructions"
              + "|talimatlar[ıi] yok say",
          Pattern.CASE_INSENSITIVE);

  public String sanitize(String rawContent) {
    if (rawContent == null) {
      throw new IllegalArgumentException("rawContent must not be null");
    }
    String withoutScripts = SCRIPT_TAG.matcher(rawContent).replaceAll("");
    String withoutHtml = HTML_TAG.matcher(withoutScripts).replaceAll("");
    String trimmed = withoutHtml.strip();

    if (trimmed.length() > MAX_CONTENT_CHARS) {
      throw new KnowledgeIngestionRejectedException(
          "content exceeds max size of " + MAX_CONTENT_CHARS + " chars");
    }
    return trimmed;
  }

  public boolean hasInstructionPattern(String sanitizedContent) {
    return INSTRUCTION_PATTERN.matcher(sanitizedContent).find();
  }
}
