package com.example.rag.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.transport.StdioServerTransport;
import io.modelcontextprotocol.server.transport.WebMvcSseServerTransport;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.ServerMcpTransport;
import org.springframework.ai.autoconfigure.mcp.server.McpServerProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class CustomMcpTransportConfig {

    @JsonIgnoreProperties(ignoreUnknown = true)
    abstract static class McpIgnoreUnknownMixin {}

    public static ObjectMapper createMcpLenientObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        mapper.addMixIn(McpSchema.ClientCapabilities.class, McpIgnoreUnknownMixin.class);
        mapper.addMixIn(McpSchema.InitializeRequest.class, McpIgnoreUnknownMixin.class);
        mapper.addMixIn(McpSchema.class, McpIgnoreUnknownMixin.class);
        mapper.addMixIn(Object.class, McpIgnoreUnknownMixin.class);
        return mapper;
    }

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "spring.ai.mcp.server", name = "stdio", havingValue = "true")
    public ServerMcpTransport stdioServerTransport() {
        return new StdioServerTransport(createMcpLenientObjectMapper());
    }

    @Bean
    @ConditionalOnProperty(prefix = "spring.ai.mcp.server", name = "stdio", havingValue = "false", matchIfMissing = true)
    public WebMvcSseServerTransport webMvcSseServerTransport(McpServerProperties properties) {
        String messageEndpoint = properties.getSseMessageEndpoint();
        if (messageEndpoint == null || messageEndpoint.isBlank()) {
            messageEndpoint = "/mcp/message";
        }
        return new WebMvcSseServerTransport(createMcpLenientObjectMapper(), messageEndpoint);
    }
}