FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8

# Install curl for healthcheck & network tools
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copy pre-packaged jar from target
COPY target/rag-mcp-server-*.jar app.jar

# Directory to persist HuggingFace local ONNX models
RUN mkdir -p /root/.spring-ai/models

EXPOSE 8080

ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-Dsun.jnu.encoding=UTF-8", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]