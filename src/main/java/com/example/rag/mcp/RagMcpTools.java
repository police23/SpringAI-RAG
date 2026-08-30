package com.example.rag.mcp;

import com.example.rag.service.KnowledgeBaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Map;

@Component
public class RagMcpTools {

    private static final Logger log = LoggerFactory.getLogger(RagMcpTools.class);

    private final KnowledgeBaseService knowledgeBaseService;

    public RagMcpTools(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @Tool(description = "Search the Qdrant knowledge base for relevant context and excerpts based on a query. Returns top matching excerpts with similarity scores and sources for LLM answering.")
    public String search_knowledge_base(
            @ToolParam(description = "Search query or question to find relevant context for") String query,
            @ToolParam(description = "Maximum number of relevant chunks to retrieve (default: 4)") Integer topK,
            @ToolParam(description = "Minimum similarity threshold between 0.0 and 1.0 (default: 0.60)") Double minScore
    ) {
        log.info("MCP Tool called: search_knowledge_base(query='{}', topK={}, minScore={})", query, topK, minScore);
        try {
            return knowledgeBaseService.searchFormattedContext(query, topK, minScore);
        } catch (Exception e) {
            log.error("Error executing search_knowledge_base: {}", e.getMessage(), e);
            return "Error querying knowledge base: " + e.getMessage();
        }
    }

    @Tool(description = "Ingest a local document file (PDF, TXT, Markdown, DOCX) from disk into the Qdrant knowledge base.")
    public String ingest_file_to_knowledge_base(
            @ToolParam(description = "Absolute or relative file path to the document file") String filePath
    ) {
        log.info("MCP Tool called: ingest_file_to_knowledge_base(filePath='{}')", filePath);
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                return "Error: File not found at path: " + filePath;
            }
            int chunks = knowledgeBaseService.ingestFile(file);
            return String.format("Success: Ingested file '%s' into Qdrant (%d chunks created & embedded).", file.getName(), chunks);
        } catch (Exception e) {
            log.error("Error executing ingest_file_to_knowledge_base: {}", e.getMessage(), e);
            return "Error ingesting file: " + e.getMessage();
        }
    }

    @Tool(description = "Refresh the Qdrant vector database: delete all existing vectors/collections and optionally re-ingest sample handbook data.")
    public String refresh_knowledge_base(
            @ToolParam(description = "Whether to re-ingest the default sample handbook data after clearing (default: true)") Boolean reIngestSample
    ) {
        log.info("MCP Tool called: refresh_knowledge_base(reIngestSample={})", reIngestSample);
        try {
            Map<String, Object> result = knowledgeBaseService.refreshKnowledgeBase(reIngestSample);
            return result.toString();
        } catch (Exception e) {
            log.error("Error executing refresh_knowledge_base: {}", e.getMessage(), e);
            return "Error refreshing knowledge base: " + e.getMessage();
        }
    }

    @Tool(description = "Get information and status about the Qdrant vector database and embedding model.")
    public String get_knowledge_base_status() {
        log.info("MCP Tool called: get_knowledge_base_status()");
        try {
            Map<String, Object> status = knowledgeBaseService.getStatus();
            return status.toString();
        } catch (Exception e) {
            log.error("Error getting knowledge base status: {}", e.getMessage(), e);
            return "Error: " + e.getMessage();
        }
    }
}