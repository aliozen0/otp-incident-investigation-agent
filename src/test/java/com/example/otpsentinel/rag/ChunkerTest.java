package com.example.otpsentinel.rag;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ChunkerTest {

  private final Chunker chunker = new Chunker();

  @Test
  void shortDocumentBecomesOneChunkPerSection() {
    String content =
        """
        ## Belirti
        Kısa bir belirti metni.

        ## Kök neden
        Kısa bir kök neden metni.
        """;

    List<DocumentChunk> chunks = chunker.chunk("DOC-1", "1", content);

    assertThat(chunks).hasSize(2);
    assertThat(chunks.get(0).sectionTitle()).isEqualTo("Belirti");
    assertThat(chunks.get(1).sectionTitle()).isEqualTo("Kök neden");
    assertThat(chunks.get(0).chunkId()).isEqualTo("DOC-1#v1#c0");
    assertThat(chunks.get(1).chunkId()).isEqualTo("DOC-1#v1#c1");
  }

  @Test
  void largeSectionSplitsWithOverlapAndKeepsSectionTitle() {
    // 30 lines of 50 distinct words each (w{line}_{word}), 1500 words total, well over 800 —
    // distinct tokens so the overlap assertion below actually proves position, not just vocabulary.
    StringBuilder body = new StringBuilder();
    for (int line = 0; line < 30; line++) {
      for (int word = 0; word < 50; word++) {
        body.append("w").append(line).append('_').append(word).append(' ');
      }
      body.append('\n');
    }
    String content = "## Kök neden\n" + body;

    List<DocumentChunk> chunks = chunker.chunk("DOC-2", "1", content);

    assertThat(chunks.size()).isGreaterThan(1);
    assertThat(chunks).allSatisfy(c -> assertThat(c.sectionTitle()).isEqualTo("Kök neden"));
    assertThat(chunks)
        .allSatisfy(c -> assertThat(c.tokenCount()).isLessThanOrEqualTo(Chunker.MAX_CHUNK_WORDS));

    // consecutive chunks overlap: chunk[i+1] starts with the same ~100-word tail chunk[i] ends
    // with (whole lines, so this is an exact prefix match, not just a shared word window).
    for (int i = 0; i + 1 < chunks.size(); i++) {
      List<String> tailWords = lastWords(chunks.get(i).content(), Chunker.OVERLAP_TARGET_WORDS);
      List<String> headWords = firstWords(chunks.get(i + 1).content(), tailWords.size());
      assertThat(headWords).isEqualTo(tailWords);
    }
  }

  @Test
  void neverSplitsInsideATableRow() {
    String tableRow = "| a | b | c | d | e | f | g | h | i | j | k | l | m | n | o | p |";
    String body = (tableRow + "\n").repeat(60); // forces a flush mid-table without the guard
    String content = "## Doğrulama\n" + body;

    List<DocumentChunk> chunks = chunker.chunk("DOC-3", "1", content);

    for (DocumentChunk chunk : chunks) {
      for (String line : chunk.content().split("\n")) {
        if (!line.isBlank()) {
          assertThat(line).isEqualTo(tableRow);
        }
      }
    }
  }

  private List<String> firstWords(String text, int n) {
    return List.of(text.strip().split("\\s+")).stream().limit(n).collect(Collectors.toList());
  }

  private List<String> lastWords(String text, int n) {
    String[] words = text.strip().split("\\s+");
    int from = Math.max(0, words.length - n);
    return List.of(words).subList(from, words.length);
  }
}
