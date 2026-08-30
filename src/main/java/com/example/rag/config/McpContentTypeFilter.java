package com.example.rag.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class McpContentTypeFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        if ("POST".equalsIgnoreCase(req.getMethod())) {
            String path = req.getRequestURI();
            if (path.startsWith("/mcp") || path.startsWith("/sse")) {
                res.setContentType("application/json;charset=UTF-8");
                res.setCharacterEncoding("UTF-8");
            }
        }

        chain.doFilter(request, response);

        if ("POST".equalsIgnoreCase(req.getMethod())) {
            String path = req.getRequestURI();
            if (path.startsWith("/mcp") || path.startsWith("/sse")) {
                if (res.getContentType() == null || res.getContentType().isBlank()) {
                    res.setContentType("application/json;charset=UTF-8");
                }
            }
        }
    }
}