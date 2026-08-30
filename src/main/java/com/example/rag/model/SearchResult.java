package com.example.rag.model;

import java.util.Map;

public record SearchResult(
    String id,
    String content,
    Double score,
    Map<String, Object> metadata
) {}