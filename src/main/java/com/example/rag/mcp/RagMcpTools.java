package com.example.rag.mcp;

import com.example.rag.service.KnowledgeBaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RagMcpTools {

    private static final Logger log = LoggerFactory.getLogger(RagMcpTools.class);

    private final KnowledgeBaseService knowledgeBaseService;

    public RagMcpTools(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @Tool(description = "Query the Qdrant knowledge base with RAG and generate an assembled English prompt containing the retrieved context, instructions, and query for LLM answering. Retrieval parameters (topK, minScore) are automatically configured from application.yml.")
    public String rag_ask(
            @ToolParam(description = "Search query or question to ask the knowledge base") String query
    ) {
        log.info("MCP Tool called: rag_ask(query='{}')", query);
        try {
            return knowledgeBaseService.buildRagPromptPayload(query, null, null).fullPrompt();
        } catch (Exception e) {
            log.error("Error executing rag_ask: {}", e.getMessage(), e);
            return "Error querying knowledge base with RAG: " + e.getMessage();
        }
    }

    @Tool(description = "Refresh the Qdrant vector database: delete all existing vectors/collections and optionally re-ingest sample handbook data.")
    public String refresh_knowledge_base(
            @ToolParam(required = false, description = "Whether to re-ingest the default sample handbook data after clearing") Boolean reIngestSample
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
}