package com.example.otpsentinel.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Splits sanitized markdown into chunks (docs/08-rag-spec.md "Chunking başlangıç hipotezi": 500-800
 * "tokens" — approximated here as whitespace-delimited words, adequate for the short fixture
 * documents this milestone ships — with an 80-120 word overlap). Never splits inside a single line,
 * which keeps a markdown table row ("tablolar bölünmez") and any other single-line structure
 * intact. A section header ({@code ## ...}) becomes the {@link DocumentChunk#sectionTitle()} for
 * every chunk carved out of that section, so, e.g., "Kök neden" and "Çözüm" land in separate chunks
 * whenever the section is large enough to need more than one.
 */
public final class Chunker {

  static final int MAX_CHUNK_WORDS = 800;
  static final int MIN_CHUNK_WORDS = 500;
  static final int OVERLAP_TARGET_WORDS = 100;

  private static final Pattern SECTION_HEADER = Pattern.compile("^##\\s+(.*)$");

  public List<DocumentChunk> chunk(String documentId, String version, String sanitizedContent) {
    List<Section> sections = splitIntoSections(sanitizedContent);
    List<DocumentChunk> chunks = new ArrayList<>();
    int index = 0;
    for (Section section : sections) {
      for (List<String> chunkLines : windowLines(section.lines())) {
        String content = String.join("\n", chunkLines).strip();
        if (content.isEmpty()) {
          continue;
        }
        chunks.add(
            new DocumentChunk(
                documentId + "#v" + version + "#c" + index,
                documentId,
                version,
                section.title(),
                content,
                wordCount(content)));
        index++;
      }
    }
    return chunks;
  }

  private List<List<String>> windowLines(List<String> lines) {
    List<List<String>> windows = new ArrayList<>();
    List<String> current = new ArrayList<>();
    int currentWords = 0;

    for (String line : lines) {
      int lineWords = wordCount(line);
      if (!current.isEmpty() && currentWords + lineWords > MAX_CHUNK_WORDS) {
        windows.add(current);
        current = overlapTail(current);
        currentWords = wordCount(String.join("\n", current));
      }
      current.add(line);
      currentWords += lineWords;
    }
    if (!current.isEmpty()) {
      windows.add(current);
    }
    return windows;
  }

  /**
   * Best-effort trailing lines totalling close to {@link #OVERLAP_TARGET_WORDS}, whole lines only.
   */
  private List<String> overlapTail(List<String> lines) {
    List<String> tail = new ArrayList<>();
    int words = 0;
    for (int i = lines.size() - 1; i >= 0 && words < OVERLAP_TARGET_WORDS; i--) {
      String line = lines.get(i);
      tail.add(0, line);
      words += wordCount(line);
    }
    return tail;
  }

  private List<Section> splitIntoSections(String content) {
    List<Section> sections = new ArrayList<>();
    String currentTitle = null;
    List<String> currentLines = new ArrayList<>();

    for (String line : content.split("\n", -1)) {
      var matcher = SECTION_HEADER.matcher(line.strip());
      if (matcher.matches()) {
        if (!currentLines.isEmpty()) {
          sections.add(new Section(currentTitle, currentLines));
        }
        currentTitle = matcher.group(1).strip();
        currentLines = new ArrayList<>();
      } else {
        currentLines.add(line);
      }
    }
    if (!currentLines.isEmpty()) {
      sections.add(new Section(currentTitle, currentLines));
    }
    return sections;
  }

  private static int wordCount(String s) {
    String trimmed = s.strip();
    return trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
  }

  private record Section(String title, List<String> lines) {}
}
