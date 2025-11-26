# 多階段構建 Dockerfile - ARM64 優化版本
# 適用於 MacBook M4 Silicon 開發和 AWS Graviton3 EKS 部署

# 第一階段：構建應用程式
FROM --platform=linux/arm64/v8 eclipse-temurin:21-jdk-alpine AS builder

# 設定構建環境變數
ENV GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.parallel=true -Dorg.gradle.configureondemand=true"
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# 安裝構建工具
RUN apk add --no-cache git

# 設定工作目錄
WORKDIR /app

# 複製 Gradle Wrapper (確保版本一致)
COPY gradlew ./
COPY gradle/ gradle/
RUN chmod +x gradlew

# 複製 Gradle 配置檔案 (利用 Docker 快取層)
COPY build.gradle settings.gradle gradle.properties ./

# 下載依賴 (獨立快取層，提高構建效率)
RUN ./gradlew dependencies --no-daemon --parallel

# 複製應用程式原始碼
COPY app/ app/

# 構建應用程式 (優化參數)
RUN ./gradlew :app:bootJar -x test --no-daemon --parallel --build-cache

# 第二階段：運行時映像 - 使用 ARM64 JRE
FROM --platform=linux/arm64/v8 eclipse-temurin:21-jre-alpine

# 安裝必要工具 (最小化安裝)
RUN apk add --no-cache wget tzdata && \
    cp /usr/share/zoneinfo/Asia/Taipei /etc/localtime && \
    echo "Asia/Taipei" > /etc/timezone && \
    apk del tzdata

# 建立應用程式使用者
RUN addgroup -g 1001 -S appuser && \
    adduser -u 1001 -S appuser -G appuser

# 設定工作目錄
WORKDIR /app

# 從構建階段複製 JAR 檔案
COPY --from=builder --chown=appuser:appuser /app/app/build/libs/*.jar app.jar

# 建立日誌目錄
RUN mkdir -p /app/logs && chown -R appuser:appuser /app

# 切換到應用程式使用者
USER appuser

# 暴露端口
EXPOSE 8080

# 健康檢查 (使用 wget 而非 curl)
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# 優化的 JVM 參數 (針對 ARM64 和 Graviton3)
ENV JAVA_OPTS="-Xms256m -Xmx512m \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+TieredCompilation \
    -XX:TieredStopAtLevel=1 \
    -XX:+UseStringDeduplication \
    -XX:+OptimizeStringConcat \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.jmx.enabled=false \
    -Dspring.main.lazy-initialization=true \
    -Dfile.encoding=UTF-8 \
    -Duser.timezone=Asia/Taipei"

ENV SPRING_PROFILES_ACTIVE=docker

# 使用 exec 形式避免 shell 包裝
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]