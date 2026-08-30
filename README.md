# RAG MCP Server với Spring AI, Qdrant Vector DB & HuggingFace Local Embeddings

Dự án này triển khai toàn bộ hệ thống **RAG (Retrieval-Augmented Generation)** dưới dạng một **MCP Server (Model Context Protocol Server)** tương thích 100% với **Antigravity**.

---

## 🌟 Các Tính Năng Chính

- **Giao thức MCP Native qua STDIO**: Phản hồi tức thì, kết nối ổn định không phụ thuộc mạng HTTP.
- **RAG Tools**:
  - `rag_ask`: Tìm kiếm ngữ cảnh và tổng hợp prompt RAG cho LLM.
  - `refresh_knowledge_base`: Xóa sạch vector database và nạp lại toàn bộ dữ liệu mẫu (fresh start).
- **Qdrant Vector Database**: Chạy trong Docker (`6333` HTTP Dashboard, `6334` gRPC).
- **HuggingFace Local Embeddings**: `sentence-transformers/all-MiniLM-L6-v2` ONNX chạy hoàn toàn offline cục bộ trong Java.

---

## 🚀 Hướng Dẫn Vận Hành

### Bước 1: Khởi động Qdrant DB bằng Docker
Trong thư mục `d:\RAG`, chạy:
```powershell
docker compose up -d qdrant
```
> Web Dashboard của Qdrant: [http://localhost:6333/dashboard](http://localhost:6333/dashboard)

---

### Bước 2: Cấu hình Antigravity
File cấu hình tại `C:\Users\<User>\.gemini\config\mcp_config.json`:

```json
{
  "mcpServers": {
    "rag-qdrant-server": {
      "command": "java",
      "args": [
        "-Dfile.encoding=UTF-8",
        "-jar",
        "d:/RAG/target/rag-mcp-server-0.0.1-SNAPSHOT.jar",
        "--spring.ai.mcp.server.stdio=true",
        "--spring.main.banner-mode=off",
        "--logging.pattern.console=",
        "--server.port=0",
        "--spring.ai.vectorstore.qdrant.host=localhost",
        "--spring.ai.vectorstore.qdrant.port=6334"
      ]
    }
  }
}
```

---

### Bước 3: Kích hoạt trên Antigravity
Nhấn nút **Refresh 🔄** trên tab **Manage MCP servers** trong Antigravity. Server sẽ kết nối và nhận diện đầy đủ **3 tools**!