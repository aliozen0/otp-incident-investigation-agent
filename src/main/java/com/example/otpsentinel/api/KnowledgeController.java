package com.example.otpsentinel.api;

import com.example.otpsentinel.api.dto.KnowledgeDocumentDto;
import com.example.otpsentinel.rag.KnowledgeIngestionRejectedException;
import com.example.otpsentinel.rag.KnowledgeIngestionService;
import com.example.otpsentinel.rag.KnowledgeRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/knowledge/documents")
@Tag(
    name = "Knowledge",
    description = "Markdown/text knowledge document upload for RAG (PDF parsing out of scope)")
public class KnowledgeController {

  private final KnowledgeIngestionService ingestionService;
  private final KnowledgeRepository repository;

  public KnowledgeController(
      KnowledgeIngestionService ingestionService, KnowledgeRepository repository) {
    this.ingestionService = ingestionService;
    this.repository = repository;
  }

  @PostMapping
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
          request.language(),
          request.tags(),
          request.content());
    } catch (KnowledgeIngestionRejectedException e) {
      throw new ApiException(
          400, "KNOWLEDGE_DOCUMENT_REJECTED", "Document rejected", e.getMessage());
    }
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new KnowledgeDocumentDto.UploadResponse(documentId, version));
  }

  @GetMapping
  public List<KnowledgeDocumentDto.ListItem> list() {
    return repository.listDocuments().stream()
        .map(
            d ->
                new KnowledgeDocumentDto.ListItem(
                    d.documentId(),
                    d.version(),
                    d.title(),
                    d.documentType().name(),
                    d.effectiveFrom()))
        .toList();
  }
}
