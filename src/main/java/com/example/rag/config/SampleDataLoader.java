package com.example.rag.config;

import com.example.rag.service.KnowledgeBaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class SampleDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SampleDataLoader.class);

    private final KnowledgeBaseService knowledgeBaseService;
    private final RagProperties ragProperties;

    public SampleDataLoader(KnowledgeBaseService knowledgeBaseService, RagProperties ragProperties) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.ragProperties = ragProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!ragProperties.isAutoIngestSample()) {
            log.info("Auto-ingest sample data is disabled.");
            return;
        }

        try {
            log.info("Checking for sample data to auto-ingest into Qdrant...");
            int chunks = knowledgeBaseService.ingestSampleHandbook();
            if (chunks > 0) {
                log.info("Auto-ingested sample handbook into Qdrant successfully ({} chunks).", chunks);
            }
        } catch (Exception e) {
            log.warn("Could not auto-ingest sample data on startup (Qdrant may not be running yet): {}", e.getMessage());
        }
    }
}