# 可觀測性故障排除指南

## 概述

本指南提供前端後端可觀測性整合系統的常見問題診斷和解決方案，包括連接問題、效能問題和數據處理問題。

## 快速診斷檢查清單

### 系統健康檢查

```bash
#!/bin/bash
# quick-health-check.sh

echo "=== 可觀測性系統健康檢查 ==="

# 1. 檢查後端服務狀態
echo "1. 檢查後端服務..."
curl -s http://localhost:8080/actuator/health | jq '.status' || echo "❌ 後端服務不可用"

# 2. 檢查 Kafka 連接 (生產環境)
echo "2. 檢查 Kafka 連接..."
curl -s http://localhost:8080/actuator/health/kafka | jq '.status' || echo "⚠️  Kafka 連接問題"

# 3. 檢查分析 API
echo "3. 測試分析 API..."
response=$(curl -s -w "%{http_code}" -X POST http://localhost:8080/api/analytics/events \
  -H "Content-Type: application/json" \
  -H "X-Trace-Id: health-check-$(date +%s)" \
  -H "X-Session-Id: health-check-session" \
  -d '[{"eventId":"health-check","eventType":"page_view","sessionId":"health-check-session","traceId":"health-check-'$(date +%s)'","timestamp":'$(date +%s000)',"data":{"page":"/health-check"}}]')

if [[ "${response: -3}" == "200" ]]; then
    echo "✅ 分析 API 正常"
else
    echo "❌ 分析 API 異常: HTTP ${response: -3}"
fi

# 4. 檢查 WebSocket 連接
echo "4. 檢查 WebSocket..."
timeout 5 wscat -c ws://localhost:8080/ws/analytics?sessionId=health-check 2>/dev/null && echo "✅ WebSocket 正常" || echo "❌ WebSocket 連接失敗"

# 5. 檢查指標端點
echo "5. 檢查指標..."
curl -s http://localhost:8080/actuator/metrics/observability.events.received >/dev/null && echo "✅ 指標端點正常" || echo "❌ 指標端點異常"

echo "=== 健康檢查完成 ==="
```

## 常見問題和解決方案

### 1. 前端事件發送問題

#### 問題: 前端事件未發送到後端

**症狀**:

- 瀏覽器網路標籤中看不到 `/api/analytics/events` 請求
- 前端控制台沒有錯誤訊息
- 後端日誌中沒有收到事件的記錄

**診斷步驟**:

```javascript
// 在瀏覽器控制台執行診斷
console.log('=== 前端可觀測性診斷 ===');

// 檢查配置
console.log('1. 檢查配置:', window.environment?.observability);

// 檢查服務狀態
const observabilityService = document.querySelector('app-root')?._ngElementStrategy?.componentRef?.instance?.observabilityService;
console.log('2. 服務狀態:', observabilityService);

// 檢查批次處理器
console.log('3. 批次處理器狀態:', observabilityService?.batchProcessor);

// 檢查本地儲存
console.log('4. 本地儲存事件:', localStorage.getItem('observability-events'));

// 手動發送測試事件
observabilityService?.trackPageView('/test-page', { test: true });
console.log('5. 測試事件已發送');
```

**解決方案**:

1. **檢查配置啟用狀態**:

```typescript
// environments/environment.ts
export const environment = {
  observability: {
    enabled: true, // 確保啟用
    // ...其他配置
  }
};
```

2. **檢查服務注入**:

```typescript
// app.component.ts
constructor(private observabilityService: ObservabilityService) {
  console.log('ObservabilityService injected:', this.observabilityService);
}
```

3. **檢查攔截器註冊**:

```typescript
// app.config.ts
export const appConfig: ApplicationConfig = {
  providers: [
    provideHttpClient(
      withInterceptors([observabilityTraceInterceptor]) // 確保攔截器已註冊
    )
  ]
};
```

#### 問題: CORS 錯誤

**症狀**:

```
Access to XMLHttpRequest at 'http://localhost:8080/api/analytics/events' 
from origin 'http://localhost:4200' has been blocked by CORS policy
```

**解決方案**:

1. **檢查後端 CORS 配置**:

```java
@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*") // 確保 CORS 配置正確
public class AnalyticsController {
    // ...
}
```

2. **全域 CORS 配置**:

```java
@Configuration
public class CorsConfiguration {
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
```

### 2. 後端事件處理問題

#### 問題: 事件接收但未處理

**症狀**:

- 後端日誌顯示 "Received X analytics events"
- 但沒有 "Successfully processed analytics events" 日誌
- 事件計數器指標不增加

**診斷步驟**:

```bash
# 檢查後端日誌
tail -f logs/genai-demo.log | grep -E "(analytics|observability)"

# 檢查事件處理指標
curl -s http://localhost:8080/actuator/metrics/observability.events.processed | jq

# 檢查錯誤指標
curl -s http://localhost:8080/actuator/metrics/observability.events.failed | jq

# 檢查 MDC 上下文
grep "correlationId" logs/genai-demo.log | tail -10
```

**解決方案**:

1. **檢查事件轉換邏輯**:

```java
@Service
public class ObservabilityEventService {
    
    public void processAnalyticsEvents(List<AnalyticsEventDto> events, String traceId, String sessionId) {
        try {
            // 添加詳細日誌
            logger.info("Processing {} events with traceId: {}", events.size(), traceId);
            
            for (AnalyticsEventDto event : events) {
                logger.debug("Processing event: {}", event.eventId());
                // 處理邏輯...
            }
            
        } catch (Exception e) {
            logger.error("Failed to process events", e);
            throw e;
        }
    }
}
```

2. **檢查領域事件發布**:

```java
@Component
public class ObservabilityEventPublisher {
    
    @EventListener
    public void handleAnalyticsEvent(UserBehaviorAnalyticsEvent event) {
        logger.info("Handling analytics event: {} for session: {}", 
            event.eventId(), event.sessionId());
        
        try {
            if ("kafka".equals(publisherType)) {
                publishToKafka(event);
            } else {
                processInMemory(event);
            }
            logger.info("Successfully handled event: {}", event.eventId());
        } catch (Exception e) {
            logger.error("Failed to handle event: {}", event.eventId(), e);
            throw e;
        }
    }
}
```

#### 問題: Kafka 連接失敗

**症狀**:

```
org.apache.kafka.common.errors.TimeoutException: 
Failed to update metadata after 60000 ms.
```

**診斷步驟**:

```bash
# 檢查 MSK 叢集狀態
aws kafka describe-cluster --cluster-arn ${MSK_CLUSTER_ARN}

# 檢查網路連接
telnet ${MSK_BOOTSTRAP_SERVERS} 9098

# 檢查 IAM 權限
aws sts get-caller-identity

# 檢查安全群組
aws ec2 describe-security-groups --group-ids ${MSK_SECURITY_GROUP_ID}
```

**解決方案**:

1. **檢查 MSK 配置**:

```yaml
spring:
  kafka:
    bootstrap-servers: ${MSK_BOOTSTRAP_SERVERS}
    security:
      protocol: SASL_SSL
    sasl:
      mechanism: AWS_MSK_IAM
    properties:
      security.protocol: SASL_SSL
      sasl.mechanism: AWS_MSK_IAM
      sasl.jaas.config: software.amazon.msk.auth.iam.IAMLoginModule required;
      sasl.client.callback.handler.class: software.amazon.msk.auth.iam.IAMClientCallbackHandler
```

2. **檢查 IAM 角色權限**:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "kafka-cluster:Connect",
        "kafka-cluster:WriteData",
        "kafka-cluster:ReadData",
        "kafka-cluster:CreateTopic",
        "kafka-cluster:DescribeTopic"
      ],
      "Resource": [
        "arn:aws:kafka:*:*:cluster/genai-demo-*",
        "arn:aws:kafka:*:*:topic/genai-demo.*.observability.*"
      ]
    }
  ]
}
```

### 3. WebSocket 連接問題

#### 問題: WebSocket 連接失敗

**症狀**:

- 前端無法建立 WebSocket 連接
- 瀏覽器控制台顯示 WebSocket 錯誤
- 即時更新功能不工作

**診斷步驟**:

```javascript
// 瀏覽器控制台測試
const testWebSocket = () => {
  const ws = new WebSocket('ws://localhost:8080/ws/analytics?sessionId=test-session');
  
  ws.onopen = () => console.log('✅ WebSocket 連接成功');
  ws.onerror = (error) => console.error('❌ WebSocket 錯誤:', error);
  ws.onclose = (event) => console.log('WebSocket 關閉:', event.code, event.reason);
  ws.onmessage = (message) => console.log('收到訊息:', message.data);
  
  // 5 秒後關閉連接
  setTimeout(() => ws.close(), 5000);
};

testWebSocket();
```

**解決方案**:

1. **檢查 WebSocket 配置 (計劃中的功能)**:

**注意：WebSocket 功能目前尚未完全實現，將在下一階段開發**

```java
// 計劃中的 WebSocket 配置
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new AnalyticsWebSocketHandler(), "/ws/analytics")
                .setAllowedOrigins("*") // 確保允許跨域
                .withSockJS(); // 添加 SockJS 支援
    }
}
```

2. **檢查防火牆和代理設定**:

```nginx
# nginx.conf
location /ws/ {
    proxy_pass http://backend;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
}
```

### 4. 效能問題

#### 問題: 事件處理延遲過高

**症狀**:

- 事件從發送到處理延遲超過 1 秒
- 批次處理積壓
- 記憶體使用率持續上升

**診斷步驟**:

```bash
# 檢查 JVM 記憶體使用
curl -s http://localhost:8080/actuator/metrics/jvm.memory.used | jq

# 檢查 GC 統計
curl -s http://localhost:8080/actuator/metrics/jvm.gc.pause | jq

# 檢查事件處理延遲
curl -s http://localhost:8080/actuator/metrics/observability.processing.latency | jq

# 檢查 Kafka 消費者延遲
curl -s http://localhost:8080/actuator/metrics/kafka.consumer.lag | jq
```

**解決方案**:

1. **調整批次處理配置**:

```yaml
genai-demo:
  observability:
    analytics:
      batch-size: 50        # 減少批次大小
      flush-interval: 15s   # 減少刷新間隔
      max-queue-size: 1000  # 限制佇列大小
```

2. **優化 JVM 參數**:

```bash
# 增加堆記憶體
export JAVA_OPTS="-Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

3. **增加 Kafka 分區數**:

```bash
# 使用 Kafka 管理工具增加分區
kafka-topics.sh --bootstrap-server ${MSK_BOOTSTRAP_SERVERS} \
  --alter --topic genai-demo.production.observability.user.behavior \
  --partitions 12
```

#### 問題: 記憶體洩漏

**症狀**:

- 記憶體使用率持續上升
- 頻繁的 Full GC
- 最終導致 OutOfMemoryError

**診斷步驟**:

```bash
# 生成堆轉儲
jcmd <pid> GC.run_finalization
jcmd <pid> VM.gc
jmap -dump:format=b,file=heap-dump.hprof <pid>

# 分析堆轉儲 (使用 Eclipse MAT 或 VisualVM)
# 查找記憶體洩漏的根本原因
```

**解決方案**:

1. **檢查事件緩存清理**:

```java
@Component
public class EventCacheManager {
    
    private final Map<String, List<ObservabilityEvent>> eventCache = new ConcurrentHashMap<>();
    
    @Scheduled(fixedRate = 300000) // 每 5 分鐘清理一次
    public void cleanupExpiredEvents() {
        long cutoffTime = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(10);
        
        eventCache.entrySet().removeIf(entry -> {
            entry.getValue().removeIf(event -> event.getTimestamp() < cutoffTime);
            return entry.getValue().isEmpty();
        });
        
        logger.debug("Cleaned up expired events, cache size: {}", eventCache.size());
    }
}
```

2. **限制事件佇列大小**:

```java
@Component
public class BatchProcessor {
    
    private final BlockingQueue<ObservabilityEvent> eventQueue = 
        new ArrayBlockingQueue<>(1000); // 限制佇列大小
    
    public void addEvent(ObservabilityEvent event) {
        if (!eventQueue.offer(event)) {
            logger.warn("Event queue is full, dropping event: {}", event.getId());
            // 可選：觸發警報
        }
    }
}
```

### 5. 數據一致性問題

#### 問題: 事件重複處理

**症狀**:

- 同一個事件被處理多次
- 業務指標計算錯誤
- 重複的領域事件

**解決方案**:

1. **實施冪等性檢查**:

```java
@Component
public class IdempotentEventProcessor {
    
    private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet();
    
    @Scheduled(fixedRate = 600000) // 每 10 分鐘清理
    public void cleanupProcessedEvents() {
        // 清理 1 小時前的事件 ID
        // 實際實現可能需要使用 Redis 或資料庫
    }
    
    public boolean isEventProcessed(String eventId) {
        return !processedEventIds.add(eventId);
    }
}
```

2. **使用資料庫唯一約束**:

```sql
CREATE TABLE processed_events (
    event_id VARCHAR(255) PRIMARY KEY,
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    session_id VARCHAR(255),
    trace_id VARCHAR(255)
);

CREATE INDEX idx_processed_events_timestamp ON processed_events(processed_at);
```

#### 問題: 事件順序錯亂

**症狀**:

- 頁面瀏覽事件在用戶操作事件之後處理
- 業務流程順序不正確

**解決方案**:

1. **使用會話 ID 作為分區鍵**:

```java
@Component
public class SessionBasedPartitioner implements Partitioner {
    
    @Override
    public int partition(String topic, Object key, byte[] keyBytes, 
                        Object value, byte[] valueBytes, Cluster cluster) {
        if (key instanceof String sessionId) {
            return Math.abs(sessionId.hashCode()) % cluster.partitionCountForTopic(topic);
        }
        return 0;
    }
}
```

2. **添加序列號**:

```java
public record AnalyticsEventDto(
    String eventId,
    String eventType,
    String sessionId,
    Long sequenceNumber, // 添加序列號
    Long timestamp,
    Map<String, Object> data
) {}
```

## 監控和警報

### 關鍵指標監控

#### 1. 系統健康指標

```bash
# 檢查系統健康指標的腳本
#!/bin/bash

echo "=== 可觀測性系統監控 ==="

# API 響應時間
api_latency=$(curl -s http://localhost:8080/actuator/metrics/http.server.requests | jq -r '.measurements[] | select(.statistic=="MEAN") | .value')
echo "API 平均響應時間: ${api_latency}ms"

# 事件處理率
events_processed=$(curl -s http://localhost:8080/actuator/metrics/observability.events.processed | jq -r '.measurements[0].value')
echo "已處理事件數: ${events_processed}"

# 錯誤率
events_failed=$(curl -s http://localhost:8080/actuator/metrics/observability.events.failed | jq -r '.measurements[0].value // 0')
echo "失敗事件數: ${events_failed}"

# 記憶體使用率
memory_used=$(curl -s http://localhost:8080/actuator/metrics/jvm.memory.used | jq -r '.measurements[0].value')
memory_max=$(curl -s http://localhost:8080/actuator/metrics/jvm.memory.max | jq -r '.measurements[0].value')
memory_usage=$(echo "scale=2; $memory_used / $memory_max * 100" | bc)
echo "記憶體使用率: ${memory_usage}%"

# 警報檢查
if (( $(echo "$api_latency > 1000" | bc -l) )); then
    echo "⚠️  警報: API 響應時間過長"
fi

if (( $(echo "$memory_usage > 80" | bc -l) )); then
    echo "⚠️  警報: 記憶體使用率過高"
fi

if (( events_failed > 10 )); then
    echo "⚠️  警報: 事件處理失敗率過高"
fi
```

#### 2. 業務指標監控

```java
@Component
public class BusinessMetricsMonitor {
    
    private final MeterRegistry meterRegistry;
    
    @Scheduled(fixedRate = 60000) // 每分鐘檢查
    public void checkBusinessMetrics() {
        // 檢查頁面瀏覽量
        Counter pageViews = meterRegistry.counter("business.page.views");
        if (pageViews.count() == 0) {
            logger.warn("No page views recorded in the last minute");
        }
        
        // 檢查用戶活動
        Counter userActions = meterRegistry.counter("business.user.actions");
        if (userActions.count() < 10) {
            logger.warn("Low user activity detected: {} actions", userActions.count());
        }
        
        // 檢查轉換率
        double conversionRate = calculateConversionRate();
        if (conversionRate < 0.02) { // 2%
            logger.warn("Low conversion rate detected: {}%", conversionRate * 100);
        }
    }
}
```

### 自動化故障恢復

#### 1. 自動重啟機制

```bash
#!/bin/bash
# auto-recovery.sh

check_service_health() {
    response=$(curl -s -w "%{http_code}" http://localhost:8080/actuator/health)
    if [[ "${response: -3}" != "200" ]]; then
        return 1
    fi
    return 0
}

restart_service() {
    echo "$(date): 檢測到服務異常，正在重啟..."
    systemctl restart genai-demo
    sleep 30
    
    if check_service_health; then
        echo "$(date): 服務重啟成功"
    else
        echo "$(date): 服務重啟失敗，需要人工介入"
        # 發送警報通知
    fi
}

# 主監控循環
while true; do
    if ! check_service_health; then
        restart_service
    fi
    sleep 60
done
```

#### 2. 自動擴展配置

```yaml
# kubernetes/hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: genai-demo-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: genai-demo-backend
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  - type: Pods
    pods:
      metric:
        name: observability_events_processing_rate
      target:
        type: AverageValue
        averageValue: "100"
```

## 日誌分析和除錯

### 結構化日誌查詢

```bash
# 查詢特定追蹤 ID 的所有日誌
grep "correlationId:trace-123" logs/genai-demo.log

# 查詢事件處理錯誤
grep -E "(ERROR|WARN).*observability" logs/genai-demo.log | tail -20

# 查詢效能問題
grep "processing.*took.*ms" logs/genai-demo.log | awk '{print $NF}' | sort -n | tail -10

# 查詢 Kafka 相關問題
grep -i kafka logs/genai-demo.log | grep -E "(ERROR|WARN)"
```

### 分散式追蹤分析

```bash
# 使用 AWS X-Ray 查詢追蹤
aws xray get-trace-summaries \
  --time-range-type TimeRangeByStartTime \
  --start-time 2024-01-01T00:00:00Z \
  --end-time 2024-01-01T23:59:59Z \
  --filter-expression 'service("genai-demo") AND error'

# 查詢特定追蹤詳情
aws xray batch-get-traces --trace-ids trace-123-456-789
```

## 效能調優建議

### 1. JVM 調優

```bash
# 生產環境推薦 JVM 參數
JAVA_OPTS="-server \
  -Xms4g -Xmx8g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+UseStringDeduplication \
  -XX:+OptimizeStringConcat \
  -XX:+UseCompressedOops \
  -XX:+UseCompressedClassPointers \
  -Djava.awt.headless=true \
  -Dspring.profiles.active=msk"
```

### 2. 資料庫調優

```sql
-- 為可觀測性相關表添加索引
CREATE INDEX CONCURRENTLY idx_observability_events_timestamp 
ON observability_events(timestamp DESC);

CREATE INDEX CONCURRENTLY idx_observability_events_session_id 
ON observability_events(session_id, timestamp DESC);

CREATE INDEX CONCURRENTLY idx_observability_events_trace_id 
ON observability_events(trace_id);

-- 定期清理舊數據
DELETE FROM observability_events 
WHERE timestamp < NOW() - INTERVAL '90 days';
```

### 3. 網路調優

```bash
# 調整 TCP 參數以優化 Kafka 連接
echo 'net.core.rmem_max = 134217728' >> /etc/sysctl.conf
echo 'net.core.wmem_max = 134217728' >> /etc/sysctl.conf
echo 'net.ipv4.tcp_rmem = 4096 65536 134217728' >> /etc/sysctl.conf
echo 'net.ipv4.tcp_wmem = 4096 65536 134217728' >> /etc/sysctl.conf
sysctl -p
```

## 聯絡支援

### 緊急聯絡資訊

- **技術支援**: <tech-support@company.com>
- **值班電話**: +1-xxx-xxx-xxxx
- **Slack 頻道**: #observability-support

### 問題回報格式

```markdown
## 問題描述
[簡要描述問題]

## 環境資訊
- 環境: [dev/test/production]
- 版本: [應用程式版本]
- 時間: [問題發生時間]

## 重現步驟
1. [步驟 1]
2. [步驟 2]
3. [步驟 3]

## 預期行為
[描述預期的正常行為]

## 實際行為
[描述實際發生的異常行為]

## 日誌和錯誤訊息
```

[貼上相關日誌]

```

## 已嘗試的解決方案
[列出已經嘗試過的解決方法]
```

## 相關文檔

- [配置指南](../observability/configuration-guide.md)
- [API 文檔](../api/observability-api.md)
- [部署指南](../deployment/observability-deployment.md)
- [效能調優指南](../performance/observability-performance.md)
