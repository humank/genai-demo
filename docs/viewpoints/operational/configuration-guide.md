# 可觀測性配置指南

## 概述

本指南詳細說明如何配置前端後端可觀測性整合系統，包括環境差異化配置、MSK 主題設定和監控配置。

## 環境配置

### 開發環境配置

#### 後端配置 (application-dev.yml)

```yaml
spring:
  profiles:
    active: dev
  datasource:
    url: jdbc:h2:file:./data/genai-demo-dev
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

# 可觀測性配置
genai-demo:
  events:
    publisher: in-memory  # 使用記憶體事件處理
    async: false         # 同步處理便於除錯
  observability:
    analytics:
      enabled: true
      storage: in-memory  # 指標存在記憶體中
      retention-minutes: 60
      batch-size: 10      # 小批次便於測試
      flush-interval: 10s # 快速刷新便於除錯
    tracing:
      enabled: true
      sample-rate: 1.0    # 100% 採樣率用於開發
    metrics:
      enabled: true
      export-interval: 30s

# 日誌配置
logging:
  level:
    solid.humank.genaidemo.infrastructure.observability: DEBUG
    solid.humank.genaidemo.application.observability: DEBUG
    org.springframework.kafka: INFO
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level [%X{correlationId:-},%X{traceId:-},%X{sessionId:-}] %logger{36} - %msg%n"
```

#### 前端配置 (environments/environment.ts)

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080',
  observability: {
    enabled: true,
    batchSize: 10,
    flushInterval: 10000, // 10 seconds
    retryAttempts: 3,
    enableDebugLogs: true,
    sampleRate: 1.0, // 100% sampling for development
    endpoints: {
      analytics: '/api/analytics/events',
      performance: '/api/analytics/performance',
      errors: '/api/monitoring/events'
    },
    webSocket: {
      url: 'ws://localhost:8080/ws/analytics',
      reconnectInterval: 5000,
      maxReconnectAttempts: 5
    }
  }
};
```

### 測試環境配置

#### 後端配置 (application-test.yml)

```yaml
spring:
  profiles:
    active: test
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop

# 可觀測性配置
genai-demo:
  events:
    publisher: in-memory
    async: false
  observability:
    analytics:
      enabled: true
      storage: in-memory
      retention-minutes: 10  # 短期保留用於測試
      batch-size: 5
      flush-interval: 5s
    tracing:
      enabled: false  # 測試時關閉追蹤以提高速度
    metrics:
      enabled: false  # 測試時關閉指標收集

logging:
  level:
    solid.humank.genaidemo: WARN
    org.springframework.kafka: ERROR
```

### 生產環境配置

#### 後端配置 (application-msk.yml)

```yaml
spring:
  profiles:
    active: msk
  kafka:
    bootstrap-servers: ${MSK_BOOTSTRAP_SERVERS}
    security:
      protocol: SASL_SSL
    sasl:
      mechanism: AWS_MSK_IAM
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 3
      batch-size: 16384
      linger-ms: 5
      buffer-memory: 33554432
    consumer:
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      group-id: ${spring.application.name}-${spring.profiles.active}
      auto-offset-reset: earliest
      enable-auto-commit: false
      properties:
        spring.json.trusted.packages: "solid.humank.genaidemo.domain.events"

# 可觀測性配置
genai-demo:
  events:
    publisher: kafka     # 使用 MSK 事件處理
    async: true         # 非同步處理
  domain-events:
    topic:
      prefix: genai-demo.${spring.profiles.active}
      partitions: 6
      replication-factor: 3
      # 可觀測性專用 topics
      observability:
        user-behavior: genai-demo.${spring.profiles.active}.observability.user.behavior
        performance-metrics: genai-demo.${spring.profiles.active}.observability.performance.metrics
        business-analytics: genai-demo.${spring.profiles.active}.observability.business.analytics
    publishing:
      enabled: true
      async: true
      dlq:
        enabled: true    # 死信佇列處理
        topic-suffix: .dlq
  observability:
    analytics:
      enabled: true
      storage: kafka      # 使用 Kafka 存儲
      retention-days: 90  # 90 天保留期
      batch-size: 100     # 大批次提高效能
      flush-interval: 30s # 30 秒刷新間隔
    tracing:
      enabled: true
      sample-rate: 0.1    # 10% 採樣率
    metrics:
      enabled: true
      export-interval: 60s

# AWS X-Ray 配置
aws:
  xray:
    tracing-name: genai-demo-${spring.profiles.active}
    context-missing: LOG_ERROR

# CloudWatch 配置
management:
  metrics:
    export:
      cloudwatch:
        namespace: GenAI/Demo/${spring.profiles.active}
        batch-size: 20
        step: 60s
        enabled: true

# 日誌配置
logging:
  level:
    solid.humank.genaidemo: INFO
    org.springframework.kafka: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{correlationId:-}] %logger{36} - %msg%n"
```

## MSK 主題配置

### 主題命名規範

```
格式: ${projectName}.${environment}.${domain}.${event}
範例: genai-demo.production.observability.user.behavior
```

### 可觀測性主題列表

#### 用戶行為分析主題

```
genai-demo.${environment}.observability.user.behavior
genai-demo.${environment}.observability.user.behavior.dlq
```

**配置參數:**

- Partitions: 6
- Replication Factor: 3
- Retention: 7 天 (開發), 30 天 (生產)
- Compression: gzip

#### 效能指標主題

```
genai-demo.${environment}.observability.performance.metrics
genai-demo.${environment}.observability.performance.metrics.dlq
```

**配置參數:**

- Partitions: 3
- Replication Factor: 3
- Retention: 3 天 (開發), 14 天 (生產)
- Compression: lz4

#### 業務分析主題

```
genai-demo.${environment}.observability.business.analytics
genai-demo.${environment}.observability.business.analytics.dlq
```

**配置參數:**

- Partitions: 6
- Replication Factor: 3
- Retention: 30 天 (開發), 90 天 (生產)
- Compression: gzip

## 監控和警報配置

### CloudWatch 指標

#### 自定義業務指標

```yaml
# application-msk.yml
management:
  metrics:
    export:
      cloudwatch:
        namespace: GenAI/Demo/Observability
        dimensions:
          Environment: ${spring.profiles.active}
          Service: ${spring.application.name}
        metrics:
          - name: observability.events.received
            description: "Number of observability events received"
          - name: observability.events.processed
            description: "Number of observability events processed"
          - name: observability.events.failed
            description: "Number of observability events failed"
          - name: observability.batch.size
            description: "Average batch size for event processing"
          - name: observability.processing.latency
            description: "Event processing latency in milliseconds"
```

### CloudWatch 警報

#### 關鍵警報

```yaml
# 事件處理失敗率過高
EventProcessingFailureRate:
  MetricName: observability.events.failed
  Threshold: 5  # 每分鐘超過 5 個失敗事件
  ComparisonOperator: GreaterThanThreshold
  EvaluationPeriods: 2
  Period: 60

# API 響應時間過長
AnalyticsAPILatency:
  MetricName: http.server.requests
  Dimensions:
    uri: /api/analytics/events
  Threshold: 1000  # 1 秒
  ComparisonOperator: GreaterThanThreshold
  Statistic: Average
  EvaluationPeriods: 3
  Period: 300

# Kafka 消費者延遲
KafkaConsumerLag:
  MetricName: kafka.consumer.lag
  Threshold: 1000  # 1000 條訊息延遲
  ComparisonOperator: GreaterThanThreshold
  EvaluationPeriods: 2
  Period: 300
```

## 安全配置

### 數據加密

#### 傳輸中加密

```yaml
# TLS 配置
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12

# Kafka TLS 配置
spring:
  kafka:
    security:
      protocol: SASL_SSL
    ssl:
      trust-store-location: ${KAFKA_TRUSTSTORE_LOCATION}
      trust-store-password: ${KAFKA_TRUSTSTORE_PASSWORD}
```

#### 靜態數據加密

```yaml
# 資料庫加密
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:5432/${DB_NAME}?sslmode=require
  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        
# KMS 配置
aws:
  kms:
    key-id: ${KMS_KEY_ID}
    region: ${AWS_REGION}
```

## 效能調優

### JVM 配置

```bash
# 生產環境 JVM 參數
JAVA_OPTS="-Xms2g -Xmx4g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+UseStringDeduplication \
  -XX:+OptimizeStringConcat \
  -Dspring.profiles.active=msk"
```

### Kafka 配置優化

```yaml
spring:
  kafka:
    producer:
      batch-size: 16384      # 16KB 批次大小
      linger-ms: 5           # 5ms 延遲以增加批次效率
      buffer-memory: 33554432 # 32MB 緩衝區
      compression-type: gzip  # 壓縮以減少網路傳輸
      acks: all              # 等待所有副本確認
      retries: 3             # 重試次數
      enable-idempotence: true # 啟用冪等性
    consumer:
      fetch-min-size: 1024   # 最小獲取大小 1KB
      fetch-max-wait: 500    # 最大等待時間 500ms
      max-poll-records: 500  # 每次輪詢最大記錄數
      session-timeout-ms: 30000 # 會話超時 30 秒
      heartbeat-interval-ms: 3000 # 心跳間隔 3 秒
```

## 故障排除

### 常見配置問題

#### 1. MSK 連接失敗

**症狀**: 無法連接到 MSK 叢集

**解決方案**:

```bash
# 檢查網路連接
telnet ${MSK_BOOTSTRAP_SERVERS} 9098

# 檢查 IAM 權限
aws sts get-caller-identity

# 檢查安全群組規則
aws ec2 describe-security-groups --group-ids ${MSK_SECURITY_GROUP_ID}
```

#### 2. 事件未被處理

**症狀**: 前端發送事件但後端未收到

**檢查清單**:

- [ ] 確認 API 端點 URL 正確
- [ ] 檢查 CORS 配置
- [ ] 驗證請求標頭格式
- [ ] 查看網路錯誤日誌

#### 3. 效能問題

**症狀**: 事件處理延遲過高

**優化建議**:

- 增加 Kafka 分區數
- 調整批次大小
- 優化 JVM 參數
- 檢查資料庫查詢效能

### 日誌分析

#### 關鍵日誌模式

```bash
# 事件接收日誌
grep "Received.*analytics events" /var/log/genai-demo/application.log

# 事件處理失敗日誌
grep "Failed to process.*event" /var/log/genai-demo/application.log

# Kafka 連接問題
grep "kafka.*connection" /var/log/genai-demo/application.log

# 效能警告
grep "processing.*took.*ms" /var/log/genai-demo/application.log
```

## 配置驗證

### 自動化驗證腳本

```bash
#!/bin/bash
# validate-observability-config.sh

echo "驗證可觀測性配置..."

# 檢查後端健康狀態
curl -f http://localhost:8080/actuator/health || exit 1

# 檢查 Kafka 連接
curl -f http://localhost:8080/actuator/health/kafka || exit 1

# 測試分析 API
curl -X POST http://localhost:8080/api/analytics/events \
  -H "Content-Type: application/json" \
  -H "X-Trace-Id: test-trace-123" \
  -H "X-Session-Id: test-session-456" \
  -d '[{"eventId":"test","eventType":"page_view","sessionId":"test-session-456","traceId":"test-trace-123","timestamp":1640995200000,"data":{"page":"/test"}}]' || exit 1

echo "配置驗證完成！"
```

### 配置檢查清單

#### 開發環境

- [ ] H2 資料庫可訪問
- [ ] 記憶體事件處理器啟用
- [ ] 除錯日誌啟用
- [ ] 本地 WebSocket 連接正常

#### 測試環境

- [ ] 測試資料庫隔離
- [ ] 可觀測性功能可選擇性啟用
- [ ] 快速事件處理配置
- [ ] 測試數據自動清理

#### 生產環境

- [ ] MSK 叢集連接正常
- [ ] SSL/TLS 加密啟用
- [ ] IAM 權限正確配置
- [ ] CloudWatch 指標正常上報
- [ ] 警報規則已設定
- [ ] 數據保留政策已配置

## 相關圖表

- \1
- \1

## 與其他視點的關聯

- **[部署視點](../deployment/README.md)**: 部署環境的配置管理
- **[開發視點](../development/README.md)**: 開發環境的配置設定
- **[安全性觀點](../../perspectives/security/README.md)**: 安全配置和加密設定

## 相關文檔

- [可觀測性系統概覽](observability-overview.md)
- [生產環境測試指南](production-observability-testing-guide.md)
- [故障排除指南](docs/troubleshooting/observability-troubleshooting.md)