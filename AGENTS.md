# AI Agent Guidelines & Architecture Reference

## Overview
This repository is a **Spring AI RAG (Retrieval-Augmented Generation) and MCP (Model Context Protocol) Server** built on top of Spring Boot 3.3.4, Spring AI, and Qdrant vector database.

## Single Source of Truth for Configuration
All RAG search parameters and vector database configurations are centrally managed in [`src/main/resources/application.yml`](file:///d:/RAG/src/main/resources/application.yml):
- `rag.collection-name`: Qdrant collection name (default: `rag_knowledge_base`)
- `rag.embedding-model`: Embedding model configuration (e.g., ONNX `all-MiniLM-L6-v2`)
- `rag.default-top-k`: Number of top relevant document chunks to retrieve (e.g., `3`)
- `rag.default-min-score`: Minimum cosine similarity score threshold (e.g., `0.5`)
- `rag.chunk-size`: Document chunk token size (e.g., `500`)
- `rag.chunk-overlap`: Overlap between adjacent chunks (e.g., `50`)
- `rag.auto-ingest-sample`: Automatically index sample handbook on startup (`true`/`false`)

## MCP Tools Guidelines
1. **`rag_ask(query)`**:
   - Only accepts the search `query` string as input.
   - Do **NOT** attempt to override `topK` or `minScore` manually; the backend automatically injects `default-top-k` and `default-min-score` from `application.yml`.
2. **`refresh_knowledge_base(reIngestSample)`**:
   - Clears existing Qdrant vectors and optionally re-ingests sample handbook files from `src/main/resources/sample-data`.

## Key Files & Structure
- [`RagProperties.java`](file:///d:/RAG/src/main/java/com/example/rag/config/RagProperties.java): Spring Boot `@ConfigurationProperties(prefix = "rag")` binding.
- [`KnowledgeBaseService.java`](file:///d:/RAG/src/main/java/com/example/rag/service/KnowledgeBaseService.java): Core business logic for document ingestion, splitting, Qdrant vector similarity search, and RAG prompt payload assembly.
- [`RagMcpTools.java`](file:///d:/RAG/src/main/java/com/example/rag/mcp/RagMcpTools.java): Exposed MCP tool endpoints annotated with `@Tool`.
- [`RagApiController.java`](file:///d:/RAG/src/main/java/com/example/rag/controller/RagApiController.java): REST API endpoints for HTTP clients.
