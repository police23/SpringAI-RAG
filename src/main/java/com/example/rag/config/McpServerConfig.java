package com.example.rag.config;

import com.example.rag.mcp.RagMcpTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpServerConfig {

    @Bean
    public ToolCallbackProvider ragToolCallbackProvider(RagMcpTools ragMcpTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(ragMcpTools)
                .build();
    }
}