package com.example.otpsentinel.api;

import com.example.otpsentinel.api.dto.KnowledgeDocumentDto;
import com.example.otpsentinel.rag.KnowledgeDocumentDetail;
import com.example.otpsentinel.rag.KnowledgeIngestionRejectedException;
import com.example.otpsentinel.rag.KnowledgeIngestionService;
import com.example.otpsentinel.rag.KnowledgeRepository;
import com.example.otpsentinel.rag.KnowledgeSearchPort;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/knowledge")
@Tag(name = "Knowledge", description = "Safe knowledge ingestion and read-only RAG exploration")
public class KnowledgeController {

  private final KnowledgeIngestionService ingestionService;
  private final KnowledgeRepository repository;
  private final KnowledgeSearchPort searchPort;

  public KnowledgeController(
      KnowledgeIngestionService ingestionService,
      KnowledgeRepository repository,
      KnowledgeSearchPort searchPort) {
    this.ingestionService = ingestionService;
    this.repository = repository;
    this.searchPort = searchPort;
  }

  @PostMapping("/documents")
  public ResponseEntity<KnowledgeDocumentDto.UploadResponse> upload(
      @RequestBody KnowledgeDocumentDto request) {
    String documentId = "UPLOAD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    String version = "1";
    try {
      ingestionService.ingest(
          documentId,
          version,
          request.title(),
          request.documentType(),
          request.provider(),
          request.effectiveFrom(),
          request.effectiveTo(),
          request.language() == null || request.language().isBlank() ? "tr" : request.language(),
          request.tags(),
          request.content());
    } catch (KnowledgeIngestionRejectedException e) {
      throw new ApiException(
          400, "KNOWLEDGE_DOCUMENT_REJECTED", "Document rejected", e.getMessage());
    }
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new KnowledgeDocumentDto.UploadResponse(documentId, version));
  }

  @GetMapping("/documents")
  public List<KnowledgeDocumentDto.ListItem> list() {
    return repository.listDocuments().stream()
        .map(
            d ->
                new KnowledgeDocumentDto.ListItem(
                    d.documentId(),
                    d.version(),
                    d.title(),
                    d.documentType().name(),
                    d.provider(),
                    d.tags(),
                    d.effectiveFrom(),
                    d.effectiveTo(),
                    d.language(),
                    d.chunkCount(),
                    d.embeddingModel(),
                    d.createdAt()))
        .toList();
  }

  @GetMapping("/documents/{documentId}/versions/{version}")
  public KnowledgeDocumentDto.Detail detail(
      @PathVariable String documentId, @PathVariable String version) {
    KnowledgeDocumentDetail detail =
        repository
            .findDocument(documentId, version)
            .orElseThrow(
                () ->
                    new ApiException(
                        404,
                        "KNOWLEDGE_DOCUMENT_NOT_FOUND",
                        "Knowledge document not found",
                        "No knowledge document exists for the requested id and version."));
    return new KnowledgeDocumentDto.Detail(
        detail.documentId(),
        detail.version(),
        detail.title(),
        detail.documentType().name(),
        detail.provider(),
        detail.tags(),
        detail.effectiveFrom(),
        detail.effectiveTo(),
        detail.language(),
        detail.createdAt(),
        detail.sanitizedContent(),
        detail.chunks().stream()
            .map(
                chunk ->
                    new KnowledgeDocumentDto.Chunk(
                        chunk.chunkId(),
                        chunk.sectionTitle(),
                        chunk.content(),
                        chunk.tokenCount(),
                        chunk.embeddingModel()))
            .toList());
  }

  @PostMapping("/search-preview")
  public KnowledgeDocumentDto.SearchPreviewResponse searchPreview(
      @RequestBody KnowledgeDocumentDto.SearchPreviewRequest request) {
    String query = request.query() == null ? "" : request.query().strip();
    int topK = request.topK() == null ? 5 : request.topK();
    if (query.length() < 3 || query.length() > 1000 || topK < 1 || topK > 5) {
      throw new ApiException(
          400,
          "INVALID_KNOWLEDGE_SEARCH",
          "Invalid knowledge search",
          "query must be 3-1000 characters and topK must be between 1 and 5");
    }
    List<KnowledgeDocumentDto.SearchResult> results =
        searchPort.searchIncidentKnowledge(query, blankToNull(request.provider()), topK).stream()
            .map(
                result ->
                    new KnowledgeDocumentDto.SearchResult(
                        result.documentId(),
                        result.version(),
                        result.title(),
                        result.chunkId(),
                        result.sectionTitle(),
                        result.similarityScore(),
                        excerpt(result.content())))
            .toList();
    return new KnowledgeDocumentDto.SearchPreviewResponse(results);
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  private String excerpt(String content) {
    return content.length() <= 600 ? content : content.substring(0, 600) + "…";
  }
}
