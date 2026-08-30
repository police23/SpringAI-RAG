package com.example.rag.config;

import com.example.rag.service.KnowledgeBaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class DataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    private final KnowledgeBaseService knowledgeBaseService;
    private final RagProperties ragProperties;

    public DataLoader(KnowledgeBaseService knowledgeBaseService, RagProperties ragProperties) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.ragProperties = ragProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!ragProperties.isAutoIngestSample()) {
            log.info("Auto-ingest data is disabled.");
            return;
        }

        try {
            log.info("Checking for data to auto-ingest into Qdrant...");
            int chunks = knowledgeBaseService.ingestFile(new File("src/main/resources/sample-data"));
            if (chunks > 0) {
                log.info("Auto-ingested data into Qdrant successfully ({} chunks).", chunks);
            }
        } catch (Exception e) {
            log.warn("Could not auto-ingest data on startup (Qdrant may not be running yet): {}", e.getMessage());
        }
    }
}
