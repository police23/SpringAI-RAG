package com.example.rag.model;

import java.util.Map;

public record RagPromptPayload(
        String query,
        String context,
        String fullPrompt,
        int contextChunkCount,
        Map<String, Object> metadata
) {
}
