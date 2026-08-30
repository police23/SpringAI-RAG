package com.example.rag.service;

import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bộ tách văn bản tích hợp giữa Spring AI và thư viện LangChain4j.
 * Sử dụng thuật toán phân tách đệ quy chính thức (DocumentSplitters.recursive) của LangChain4j:
 * Cắt thông minh theo phân cấp: Đoạn văn (\n\n) -> Dòng (\n) -> Dấu câu -> Từ -> Ký tự kèm Overlap.
 */
public class LangChainRecursiveTextSplitter extends TextSplitter {

    /**
     * Đối tượng DocumentSplitter thực tế của thư viện LangChain4j chịu trách nhiệm thuật toán cắt.
     */
    private final DocumentSplitter splitter;

    /**
     * Hàm khởi tạo (Constructor).
     *
     * @param maxSegmentSizeInChars Kích thước tối đa của mỗi chunk (tính theo ký tự, tối thiểu 50).
     * @param maxOverlapSizeInChars Độ dài chồng lấn (overlap) gối đầu giữa 2 chunk liền kề.
     */
    public LangChainRecursiveTextSplitter(int maxSegmentSizeInChars, int maxOverlapSizeInChars) {
        // Khởi tạo bộ tách đệ quy của LangChain4j với kích thước chunk và overlap
        this.splitter = DocumentSplitters.recursive(
                Math.max(50, maxSegmentSizeInChars),
                Math.max(0, maxOverlapSizeInChars)
        );
    }

    /**
     * Xử lý cắt danh sách tài liệu Document của Spring AI thành các chunks nhỏ hơn.
     * Hàm này chuyển đổi qua lại giữa Spring AI và LangChain4j để giữ nguyên Metadata của file.
     *
     * @param documents Danh sách tài liệu thô ban đầu (đọc từ file PDF, DOCX, TXT qua Tika Reader).
     * @return Danh sách các chunks Document của Spring AI đã được chia nhỏ sẵn sàng nạp vào Vector Store.
     */
    @Override
    public List<Document> apply(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return Collections.emptyList();
        }

        List<Document> result = new ArrayList<>();
        for (Document doc : documents) {
            if (doc.getText() == null || doc.getText().isBlank()) {
                continue;
            }

            // 1. Chuyển Metadata từ Spring AI sang Metadata của LangChain4j
            Metadata metadata = doc.getMetadata() != null 
                    ? new Metadata(doc.getMetadata()) 
                    : new Metadata();

            // 2. Đóng gói thành Document của LangChain4j
            dev.langchain4j.data.document.Document langchainDoc = 
                    dev.langchain4j.data.document.Document.from(doc.getText(), metadata);

            // 3. Thực thi thuật toán cắt đệ quy của LangChain4j
            List<TextSegment> segments = splitter.split(langchainDoc);

            // 4. Chuyển đổi từng TextSegment của LangChain4j ngược lại thành Document của Spring AI
            for (TextSegment segment : segments) {
                result.add(new Document(segment.text(), segment.metadata().toMap()));
            }
        }
        return result;
    }

    /**
     * Phương thức kế thừa bắt buộc từ lớp cha TextSplitter của Spring AI.
     * Dùng khi chỉ cần cắt một chuỗi String đơn thuần thành danh sách các chuỗi con.
     *
     * @param text Đoạn văn bản dạng String cần chia nhỏ.
     * @return Danh sách các chuỗi text con đã được chia nhỏ kèm overlap.
     */
    @Override
    protected List<String> splitText(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        // Tạo Document LangChain từ chuỗi text và thực hiện cắt
        dev.langchain4j.data.document.Document langchainDoc = 
                dev.langchain4j.data.document.Document.from(text);
        List<TextSegment> segments = splitter.split(langchainDoc);
        
        // Trả về danh sách chuỗi String
        return segments.stream().map(TextSegment::text).toList();
    }
}
