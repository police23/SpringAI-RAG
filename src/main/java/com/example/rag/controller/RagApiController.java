package com.example.rag.controller;

import com.example.rag.model.SearchResult;
import com.example.rag.service.KnowledgeBaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rag")
@CrossOrigin(origins = "*")
public class RagApiController {

    private static final Logger log = LoggerFactory.getLogger(RagApiController.class);

    private final KnowledgeBaseService knowledgeBaseService;

    public RagApiController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    /**
     * Semantic Search endpoint.
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam("query") String query,
            @RequestParam(value = "topK", required = false) Integer topK,
            @RequestParam(value = "minScore", required = false) Double minScore
    ) {
        List<SearchResult> results = knowledgeBaseService.search(query, topK, minScore);
        String formattedContext = knowledgeBaseService.searchFormattedContext(query, topK, minScore);

        Map<String, Object> response = new HashMap<>();
        response.put("query", query);
        response.put("count", results.size());
        response.put("results", results);
        response.put("formattedContext", formattedContext);

        return ResponseEntity.ok(response);
    }

    /**
     * Ingest local file by path.
     */
    @PostMapping("/ingest-file")
    public ResponseEntity<Map<String, Object>> ingestFilePath(@RequestParam("filePath") String filePath) {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File not found: " + filePath));
        }

        int chunks = knowledgeBaseService.ingestFile(file);
        return ResponseEntity.ok(Map.of(
                "message", "Successfully ingested file",
                "fileName", file.getName(),
                "chunks", chunks
        ));
    }

    /**
     * Upload document file via multipart.
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile multipartFile) {
        if (multipartFile.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Uploaded file is empty"));
        }

        try {
            String originalFilename = multipartFile.getOriginalFilename() != null ? multipartFile.getOriginalFilename() : "document.txt";
            File tempFile = File.createTempFile("rag_upload_", "_" + originalFilename);
            tempFile.deleteOnExit();

            try (InputStream is = multipartFile.getInputStream()) {
                Files.copy(is, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            int chunks = knowledgeBaseService.ingestFile(tempFile);
            return ResponseEntity.ok(Map.of(
                "message", "Successfully uploaded and indexed file",
                "fileName", originalFilename,
                "chunks", chunks
            ));
        } catch (Exception e) {
            log.error("Failed to process uploaded file: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Refresh knowledge base: clear all vectors and re-ingest sample data.
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(
            @RequestParam(value = "reIngestSample", required = false, defaultValue = "true") Boolean reIngestSample
    ) {
        Map<String, Object> result = knowledgeBaseService.refreshKnowledgeBase(reIngestSample);
        return ResponseEntity.ok(result);
    }
}