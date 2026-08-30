package com.example.rag.service;

import com.example.rag.config.RagProperties;
import com.example.rag.model.RagPromptPayload;
import com.example.rag.model.SearchResult;
import io.qdrant.client.QdrantClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class KnowledgeBaseService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseService.class);

    private final VectorStore vectorStore;
    private final RagProperties ragProperties;

    public KnowledgeBaseService(VectorStore vectorStore, RagProperties ragProperties) {
        this.vectorStore = vectorStore;
        this.ragProperties = ragProperties;
    }

    /**
     * Semantic similarity search in Qdrant vector database.
     */
    public List<SearchResult> search(String query, Integer topK, Double minScore) {
        int k = (topK != null && topK > 0) ? topK : ragProperties.getDefaultTopK();
        double threshold = (minScore != null && minScore >= 0.0 && minScore <= 1.0) ? minScore : ragProperties.getDefaultMinScore();

        log.info("Searching knowledge base for query: '{}', topK: {}, minScore: {}", query, k, threshold);

        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(k)
                .similarityThreshold(threshold)
                .build();

        List<Document> documents = vectorStore.similaritySearch(searchRequest);
        if (documents == null || documents.isEmpty()) {
            log.info("No matching documents found above threshold {}", threshold);
            return Collections.emptyList();
        }

        return documents.stream().map(doc -> {
            Double score = 1.0;
            if (doc.getMetadata() != null) {
                Object scoreObj = doc.getMetadata().get("distance");
                if (scoreObj == null) {
                    scoreObj = doc.getMetadata().get("score");
                }
                if (scoreObj instanceof Number num) {
                    score = num.doubleValue();
                }
            }
            return new SearchResult(
                    doc.getId(),
                    doc.getText(),
                    score,
                    doc.getMetadata()
            );
        }).collect(Collectors.toList());
    }

    /**
     * Search and format into a structured prompt-ready context string for LLMs.
     */
    public String searchFormattedContext(String query, Integer topK, Double minScore) {
        List<SearchResult> results = search(query, topK, minScore);

        if (results.isEmpty()) {
            return "No relevant context found in the knowledge base for query: \"" + query + "\".";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== RELEVANT CONTEXT FROM KNOWLEDGE BASE ===\n\n");

        for (int i = 0; i < results.size(); i++) {
            SearchResult res = results.get(i);
            String source = res.metadata() != null ? String.valueOf(res.metadata().getOrDefault("source", "unknown")) : "unknown";
            String title = res.metadata() != null ? String.valueOf(res.metadata().getOrDefault("title", source)) : source;

            sb.append(String.format("[Source %d]: %s (Score: %.3f)\n", (i + 1), title, res.score() != null ? res.score() : 1.0));
            sb.append("--------------------------------------------------\n");
            sb.append(res.content() != null ? res.content().trim() : "").append("\n\n");
        }

        sb.append("=== END OF CONTEXT ===");
        return sb.toString();
    }

    /**
     * Build structured RAG prompt payload with System Prompt, retrieved Context, and User Prompt.
     */
    public RagPromptPayload buildRagPromptPayload(String query, Integer topK, Double minScore) {
        List<SearchResult> searchResults = search(query, topK, minScore);
        String contextStr;

        if (searchResults.isEmpty()) {
            contextStr = "No matching excerpts found in knowledge base.";
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < searchResults.size(); i++) {
                SearchResult res = searchResults.get(i);
                String source = res.metadata() != null ? String.valueOf(res.metadata().getOrDefault("source", "unknown")) : "unknown";
                sb.append(String.format("[%d] Source: %s (Relevance Score: %.3f)\n%s\n\n",
                        i + 1, source, res.score() != null ? res.score() : 1.0,
                        res.content() != null ? res.content().trim() : ""));
            }
            contextStr = sb.toString().trim();
        }

        String fullPrompt = ragProperties.getPromptTemplate()
                .replace("{context}", contextStr)
                .replace("{question}", query);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("topK", (topK != null && topK > 0) ? topK : ragProperties.getDefaultTopK());
        metadata.put("minScore", (minScore != null && minScore >= 0.0 && minScore <= 1.0) ? minScore : ragProperties.getDefaultMinScore());
        metadata.put("collectionName", ragProperties.getCollectionName());

        return new RagPromptPayload(
                query,
                contextStr,
                fullPrompt,
                searchResults.size(),
                metadata
        );
    }

    /**
     * Ingest document file(s) or all files in a directory (such as sample-data) into Qdrant vector database.
     */
    public int ingestFile(File file) {
        if (file == null) {
            file = new File("src/main/resources/sample-data");
        }

        if (!file.exists()) {
            throw new IllegalArgumentException("File or directory does not exist: " + file.getAbsolutePath());
        }

        if (file.isDirectory()) {
            File[] files = file.listFiles((dir, name) -> !name.startsWith("."));
            int totalChunks = 0;
            if (files != null) {
                for (File f : files) {
                    if (f.isFile()) {
                        totalChunks += ingestSingleFile(f);
                    }
                }
            }
            log.info("Finished ingesting directory '{}' ({} total chunks)", file.getAbsolutePath(), totalChunks);
            return totalChunks;
        }

        return ingestSingleFile(file);
    }

    private int ingestSingleFile(File file) {
        log.info("Reading file: {}", file.getAbsolutePath());
        Resource resource = new FileSystemResource(file);
        TikaDocumentReader reader = new TikaDocumentReader(resource);
        List<Document> rawDocs = reader.get();

        LangChainRecursiveTextSplitter splitter = new LangChainRecursiveTextSplitter(
                ragProperties.getChunkSize(),
                ragProperties.getChunkOverlap()
        );
        List<Document> chunks = splitter.apply(rawDocs);

        for (int i = 0; i < chunks.size(); i++) {
            Document chunk = chunks.get(i);
            chunk.getMetadata().put("source", file.getName());
            chunk.getMetadata().put("file_path", file.getAbsolutePath());
            chunk.getMetadata().put("file_size_bytes", (int) Math.min(file.length(), Integer.MAX_VALUE));
            chunk.getMetadata().put("chunk_index", i + 1);
            chunk.getMetadata().put("total_chunks", chunks.size());
            chunk.getMetadata().put("ingested_at", Instant.now().toString());
        }

        vectorStore.accept(chunks);
        log.info("Successfully indexed file '{}' ({} chunks) into Qdrant collection '{}'", 
                file.getName(), chunks.size(), ragProperties.getCollectionName());
        return chunks.size();
    }

    /**
     * Clear all vectors in Qdrant, re-initialize collection, and optionally re-ingest sample data.
     */
    public Map<String, Object> refreshKnowledgeBase(Boolean reIngestSample) {
        String collectionName = ragProperties.getCollectionName();
        log.warn("Refreshing knowledge base for collection: '{}'...", collectionName);
        Map<String, Object> result = new LinkedHashMap<>();

        try {
            if (vectorStore instanceof QdrantVectorStore qdrantVectorStore) {
                Optional<QdrantClient> clientOpt = qdrantVectorStore.getNativeClient();
                if (clientOpt.isPresent()) {
                    QdrantClient client = clientOpt.get();
                    try {
                        client.deleteCollectionAsync(collectionName).get();
                        log.info("Deleted Qdrant collection '{}' successfully", collectionName);
                    } catch (Exception e) {
                        log.warn("Could not delete collection '{}' (might not exist yet): {}", collectionName, e.getMessage());
                    }
                    qdrantVectorStore.afterPropertiesSet();
                    log.info("Re-initialized Qdrant collection '{}' schema", collectionName);
                }
            }

            int reIngestedChunks = 0;
            boolean shouldReIngest = (reIngestSample != null) ? reIngestSample : ragProperties.isAutoIngestSample();
            if (shouldReIngest) {
                reIngestedChunks = ingestFile(new File("src/main/resources/sample-data"));
            }

            result.put("status", "SUCCESS");
            result.put("message", "Knowledge base cleared and re-initialized successfully");
            result.put("collection", collectionName);
            result.put("reIngestedSampleChunks", reIngestedChunks);
        } catch (Exception e) {
            log.error("Failed to refresh knowledge base: {}", e.getMessage(), e);
            result.put("status", "ERROR");
            result.put("error", e.getMessage());
        }

        return result;
    }
}