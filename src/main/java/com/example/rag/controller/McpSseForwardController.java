package com.example.rag.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class McpSseForwardController {

    private static final Logger log = LoggerFactory.getLogger(McpSseForwardController.class);

    @PostMapping("/sse")
    public String forwardSsePost() {
        log.debug("Forwarding POST /sse to /mcp/message for MCP client compatibility");
        return "forward:/mcp/message";
    }
}