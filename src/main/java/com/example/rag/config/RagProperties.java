package com.example.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    /**
     * Tên collection trong Qdrant Vector Database
     */
    private String collectionName;

    /**
     * Tên model embedding đang sử dụng
     */
    private String embeddingModel;

    /**
     * Số lượng kết quả chunk tối đa trả về khi tìm kiếm tương đồng
     */
    private int defaultTopK;

    /**
     * Ngưỡng độ tương đồng tối thiểu (0.0 -> 1.0)
     */
    private double defaultMinScore;

    /**
     * Kích thước mỗi chunk (tính theo token/từ)
     */
    private int chunkSize;

    /**
     * Độ chồng lấn (overlap) giữa 2 chunk liền kề (tính theo token/từ)
     */
    private int chunkOverlap;

    /**
     * Kích thước ký tự tối thiểu của một chunk
     */
    private int minChunkSizeChars;

    /**
     * Tự động nạp dữ liệu mẫu handbook khi khởi động
     */
    private boolean autoIngestSample;

    public RagProperties() {
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public int getDefaultTopK() {
        return defaultTopK;
    }

    public void setDefaultTopK(int defaultTopK) {
        this.defaultTopK = defaultTopK;
    }

    public double getDefaultMinScore() {
        return defaultMinScore;
    }

    public void setDefaultMinScore(double defaultMinScore) {
        this.defaultMinScore = defaultMinScore;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getChunkOverlap() {
        return chunkOverlap;
    }

    public void setChunkOverlap(int chunkOverlap) {
        this.chunkOverlap = chunkOverlap;
    }

    public int getMinChunkSizeChars() {
        return minChunkSizeChars;
    }

    public void setMinChunkSizeChars(int minChunkSizeChars) {
        this.minChunkSizeChars = minChunkSizeChars;
    }

    public boolean isAutoIngestSample() {
        return autoIngestSample;
    }

    public void setAutoIngestSample(boolean autoIngestSample) {
        this.autoIngestSample = autoIngestSample;
    }
}
