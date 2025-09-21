
# Deployment

## 概述

本指南說明如何使用 Docker Deployment GenAI Demo 應用程式。我們提供了針對 ARM64 架構優化的輕量化 Docker 映像。

## Requirements

### Requirements

- **CPU**: ARM64 架構 (Apple Silicon M1/M2/M3 或 ARM64 伺服器)
- **記憶體**: 最少 1GB RAM (recommendations 2GB+)
- **儲存空間**: 最少 2GB 可用空間

### Requirements

- **Docker**: 20.10+
- **Docker Compose**: 2.0+
- **作業系統**: macOS (Apple Silicon)、Linux ARM64

## 快速開始

### 1. 構建映像

```bash
# 使用提供的構建腳本 (推薦)
./docker/docker-build.sh

# 或手動構建
docker build --platform linux/arm64 -t genai-demo:latest .
```

### 2. 啟動服務

```bash
# 啟動所有服務
docker-compose up -d

# 查看服務狀態
docker-compose ps

# 查看Logging
docker-compose logs -f genai-demo
```

### 3. 訪問應用

- **API 文檔**: <http://localhost:8080/swagger-ui/index.html>
- **Health Check**: <http://localhost:8080/actuator/health>
- **H2 Repository控制台**: <http://localhost:8080/h2-console>

## 映像優化特色

### 1. ARM64 原生支援

- 使用 ARM64 原生基礎映像
- 針對 Apple Silicon 和 ARM64 伺服器優化
- 避免 x86 模擬帶來的效能損失

### 2. 多階段構建

```dockerfile
# 構建階段 - 包含完整的 Gradle 和 JDK
FROM gradle:8.5-jdk21-alpine AS builder

# 運行階段 - 僅包含 JRE 和應用程式
FROM eclipse-temurin:21-jre-alpine
```

### 3. JVM 優化

```bash
# 針對容器Environment優化的 JVM 參數
JAVA_OPTS="-Xms256m -Xmx512m \
    -XX:+UseSerialGC \
    -XX:+TieredCompilation \
    -XX:TieredStopAtLevel=1 \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0"
```

### 4. Security增強

- 使用非 root 用戶執行應用程式
- 最小化基礎映像 (Alpine Linux)
- 移除不必要的套件和工具

## 配置說明

### Environment變數

| 變數名稱 | 預設值 | 說明 |
|---------|--------|------|
| `SPRING_PROFILES_ACTIVE` | `docker` | Spring Boot 設定檔 |
| `JAVA_OPTS` | 見上方 | JVM 啟動參數 |
| `SPRING_DATASOURCE_URL` | `jdbc:h2:mem:genaidemo` | Repository連接 URL |

### 資料持久化

```yaml
# docker-compose.yml 中的 volume 配置
volumes:
  - ./logs:/app/logs  # Logging持久化
```

### Health Check

```yaml
healthcheck:
  test: ["CMD-SHELL", "wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 60s
```

## Troubleshooting

### 1. 映像構建失敗

**問題**: 構建過程中出現架構不匹配錯誤

```bash
# 解決方案：明確指定平台
docker build --platform linux/arm64 -t genai-demo:latest .
```

### 2. 容器啟動失敗

**問題**: 記憶體不足

```bash
# 解決方案：調整 JVM 記憶體參數
export JAVA_OPTS="-Xms128m -Xmx256m"
docker-compose up -d
```

### 3. Health Check失敗

**問題**: 應用程式啟動時間過長

```bash
# 解決方案：增加啟動等待時間
# 在 docker-compose.yml 中調整 start_period
healthcheck:
  start_period: 120s  # 增加到 2 分鐘
```

### 4. Logging查看

```bash
# 查看應用程式Logging
docker-compose logs -f genai-demo

# 查看特定時間範圍的Logging
docker-compose logs --since="2024-01-01T00:00:00" genai-demo

# 查看最後 100 行Logging
docker-compose logs --tail=100 genai-demo
```

## 效能調優

### 1. JVM 記憶體調整

根據可用記憶體調整 JVM 參數：

```bash
# 1GB RAM 系統
export JAVA_OPTS="-Xms128m -Xmx256m"

# 2GB RAM 系統
export JAVA_OPTS="-Xms256m -Xmx512m"

# 4GB+ RAM 系統
export JAVA_OPTS="-Xms512m -Xmx1024m"
```

### 2. 垃圾收集器選擇

```bash
# 低記憶體Environment (< 1GB)
-XX:+UseSerialGC

# 中等記憶體Environment (1-4GB)
-XX:+UseG1GC

# 高記憶體Environment (4GB+)
-XX:+UseZGC  # Java 17+
```

### 3. 啟動時間優化

```bash
# 快速啟動參數
-XX:+TieredCompilation \
-XX:TieredStopAtLevel=1 \
-Dspring.main.lazy-initialization=true \
-Dspring.jmx.enabled=false
```

## Deployment

### 1. Security檢查清單

- [ ] 使用非 root 用戶執行
- [ ] 移除開發工具和除錯端點
- [ ] 設定適當的Resource限制
- [ ] 啟用Logging輪轉
- [ ] 配置Monitoring和告警

### Resources

```yaml
# docker-compose.yml
services:
  genai-demo:
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 1G
        reservations:
          cpus: '0.5'
          memory: 512M
```

### 3. Logging管理

```yaml
# Logging輪轉配置
logging:
  driver: "json-file"
  options:
    max-size: "100m"
    max-file: "3"
```

## 相關文檔

- [README.md](../README.md) - 專案概述
- \1 - 完整的專案成果summary
- [API_VERSIONING_STRATEGY.md](../api/API_VERSIONING_STRATEGY.md) - API 版本管理
