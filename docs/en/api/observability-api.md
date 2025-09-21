
# Observability API 文檔

## 概述

This document描述了前端後端Observability整合系統的 API 端點，包括用戶行為Tracing、效能Monitoring和業務分析功能。

## 新增 API 端點

### 分析事件 API

#### POST /api/analytics/events

接收前端發送的用戶行為分析事件。

**請求標頭**

```
Content-Type: application/json
X-Trace-Id: string (必需) - Tracing ID，用於端到端Tracing
X-Session-Id: string (必需) - 會話 ID
X-Correlation-Id: string (可選) - 關聯 ID
```

**請求體**

```json
[
  {
    "eventId": "string",
    "eventType": "page_view|user_action|business_event",
    "sessionId": "string",
    "userId": "string (可選)",
    "traceId": "string",
    "timestamp": 1640995200000,
    "data": {
      "page": "/products",
      "action": "click",
      "element": "add-to-cart-button",
      "productId": "PROD-123",
      "category": "electronics"
    }
  }
]
```

**響應**

- **200 OK**: 事件成功接收
- **400 Bad Request**: 請求格式錯誤
- **500 Internal Server Error**: 服務器內部錯誤

**範例請求**

```bash
curl -X POST http://localhost:8080/api/analytics/events \
  -H "Content-Type: application/json" \
  -H "X-Trace-Id: trace-1640995200000-abc123" \
  -H "X-Session-Id: session-xyz789" \
  -d '[
    {
      "eventId": "event-001",
      "eventType": "page_view",
      "sessionId": "session-xyz789",
      "traceId": "trace-1640995200000-abc123",
      "timestamp": 1640995200000,
      "data": {
        "page": "/products",
        "referrer": "/home",
        "userAgent": "Mozilla/5.0..."
      }
    }
  ]'
```

#### POST /api/analytics/performance

接收前端發送的效能Metrics數據。

**請求標頭**

```
Content-Type: application/json
X-Trace-Id: string (必需) - Tracing ID
X-Session-Id: string (必需) - 會話 ID
```

**請求體**

```json
[
  {
    "metricId": "string",
    "metricType": "lcp|fid|cls|ttfb|page_load",
    "value": 1500.5,
    "page": "/products",
    "sessionId": "string",
    "traceId": "string",
    "timestamp": 1640995200000,
    "metadata": {
      "viewport": "1920x1080",
      "connection": "4g",
      "device": "desktop"
    }
  }
]
```

**響應**

- **200 OK**: Metrics成功接收
- **400 Bad Request**: 請求格式錯誤
- **500 Internal Server Error**: 服務器內部錯誤

**範例請求**

```bash
curl -X POST http://localhost:8080/api/analytics/performance \
  -H "Content-Type: application/json" \
  -H "X-Trace-Id: trace-1640995200000-abc123" \
  -H "X-Session-Id: session-xyz789" \
  -d '[
    {
      "metricId": "metric-001",
      "metricType": "lcp",
      "value": 1200.5,
      "page": "/products",
      "sessionId": "session-xyz789",
      "traceId": "trace-1640995200000-abc123",
      "timestamp": 1640995200000
    }
  ]'
```

#### GET /api/analytics/stats

查詢分析統計數據。

**請求參數**

- `timeRange` (必需): 時間範圍 (1h, 24h, 7d, 30d)
- `filter` (可選): 篩選條件
- `page` (可選): 頁碼，預設 0
- `size` (可選): 頁面大小，預設 20

**請求標頭**

```
X-Trace-Id: string (可選) - Tracing ID
```

**響應**

```json
{
  "timeRange": "24h",
  "totalEvents": 15420,
  "uniqueUsers": 1250,
  "pageViews": 8900,
  "userActions": 6520,
  "averageSessionDuration": 180000,
  "topPages": [
    {
      "page": "/products",
      "views": 3200,
      "uniqueUsers": 890
    }
  ],
  "performanceMetrics": {
    "averageLCP": 1200.5,
    "averageFID": 85.2,
    "averageCLS": 0.05
  }
}
```

### Monitoring事件 API (現有端點擴展)

#### POST /api/monitoring/events

接收前端 JavaScript 錯誤和Monitoring事件。

**請求體**

```json
{
  "eventType": "javascript_error|api_error|network_error",
  "message": "string",
  "stack": "string (可選)",
  "url": "string (可選)",
  "userAgent": "string (可選)",
  "timestamp": 1640995200000,
  "sessionId": "string",
  "traceId": "string",
  "metadata": {
    "component": "ProductList",
    "action": "loadProducts",
    "errorCode": "NETWORK_TIMEOUT"
  }
}
```

### WebSocket 即時分析 API

#### WS /ws/analytics

建立 WebSocket 連接以接收即時分析更新。

**連接參數**

- `sessionId`: 會話 ID
- `channels`: 訂閱頻道列表 (user-behavior, performance, business-metrics)

**訊息格式**

```json
{
  "type": "analytics_update",
  "channel": "user-behavior",
  "data": {
    "eventType": "page_view",
    "count": 1,
    "timestamp": 1640995200000,
    "metadata": {
      "page": "/products",
      "totalViews": 3201
    }
  }
}
```

**範例 JavaScript Customer端**

```javascript
const socket = new WebSocket('ws://localhost:8080/ws/analytics?sessionId=session-xyz789&channels=user-behavior,performance');

socket.onmessage = function(event) {
  const data = JSON.parse(event.data);
  console.log('Real-time update:', data);
};
```

## 數據模型

### AnalyticsEventDto

```java
public record AnalyticsEventDto(
    @NotBlank String eventId,
    @NotBlank String eventType,
    @NotBlank String sessionId,
    String userId,
    @NotBlank String traceId,
    @NotNull Long timestamp,
    @NotNull Map<String, Object> data
) {}
```

### PerformanceMetricDto

```java
public record PerformanceMetricDto(
    @NotBlank String metricId,
    @NotBlank String metricType,
    @NotNull Double value,
    @NotBlank String page,
    @NotBlank String sessionId,
    @NotBlank String traceId,
    @NotNull Long timestamp,
    Map<String, Object> metadata
) {}
```

### AnalyticsStatsDto

```java
public record AnalyticsStatsDto(
    String timeRange,
    Long totalEvents,
    Long uniqueUsers,
    Long pageViews,
    Long userActions,
    Long averageSessionDuration,
    List<PageStatsDto> topPages,
    PerformanceStatsDto performanceMetrics
) {}
```

## 錯誤處理

### 錯誤響應格式

```json
{
  "errorCode": "ANALYTICS_VALIDATION_ERROR",
  "message": "Invalid event data format",
  "timestamp": "2024-01-01T12:00:00Z",
  "traceId": "trace-1640995200000-abc123",
  "details": {
    "field": "eventType",
    "rejectedValue": "invalid_type",
    "allowedValues": ["page_view", "user_action", "business_event"]
  }
}
```

### 常見錯誤碼

- `ANALYTICS_VALIDATION_ERROR`: 數據驗證失敗
- `ANALYTICS_PROCESSING_ERROR`: 事件處理失敗
- `ANALYTICS_RATE_LIMIT_EXCEEDED`: 請求頻率超限
- `ANALYTICS_TRACE_ID_MISSING`: 缺少Tracing ID
- `ANALYTICS_SESSION_INVALID`: 無效會話 ID

## 安全考量

### 認證和授權

- 所有 API 端點都需要有效的會話
- Tracing ID 用於請求關聯，不包含敏感資訊
- 用戶數據在傳輸和存儲時都會加密

### 數據隱私

- PII 數據會自動遮罩
- 用戶行為數據匿名化處理
- 符合 GDPR 和其他隱私法規要求

### 速率限制

- 每個會話每分鐘最多 1000 個事件
- 批次請求最多包含 100 個事件
- WebSocket 連接每個會話限制 5 個

## 效能考量

### 批次處理

- 前端recommendations每 30 秒或累積 50 個事件後發送
- 後端支援批次處理以提高效能
- 關鍵事件（如購買）可立即發送

### 快取Policy

- 統計查詢結果快取 5 分鐘
- 即時Metrics快取 30 秒
- 使用 Redis 進行分散式快取

### MonitoringMetrics

- API 響應時間 < 200ms (95th percentile)
- 事件處理延遲 < 100ms
- WebSocket 連接穩定性 > 99.9%

## Testing

### Testing

```bash
# Testing
./gradlew test --tests "*AnalyticsController*"

# Testing
./gradlew test --tests "*ObservabilityEventService*"
```

### Testing

```bash
# Testing
./gradlew test --tests "*ObservabilityIntegration*"
```

### Testing

```bash
# Testing
k6 run scripts/load-test-analytics.js
```

## Deployment

### 開發Environment

```yaml
genai-demo:
  events:
    publisher: in-memory
  observability:
    analytics:
      enabled: true
      batch-size: 10
      flush-interval: 10s
```

### 生產Environment

```yaml
genai-demo:
  events:
    publisher: kafka
  observability:
    analytics:
      enabled: true
      batch-size: 100
      flush-interval: 30s
      retention-days: 90
```

## Troubleshooting

### 常見問題

1. **事件未被處理**
   - 檢查Tracing ID 格式
   - 驗證會話 ID 有效性
   - 查看應用程式Logging

2. **WebSocket 連接失敗**
   - 檢查網路連接
   - 驗證會話參數
   - 查看瀏覽器控制台錯誤

3. **效能Metrics異常**
   - 檢查Metrics類型拼寫
   - 驗證數值範圍
   - 確認頁面 URL 格式

### Logging查詢

```bash
# 查看分析事件處理Logging
kubectl logs -f deployment/genai-demo-backend | grep "analytics"

# 查看錯誤Logging
kubectl logs -f deployment/genai-demo-backend | grep "ERROR.*observability"
```

## 相關文檔

- [Observability配置指南](../observability/configuration-guide.md)
- [前端 SDK 使用指南](../development/frontend-observability-sdk.md)
- [故障排除指南](../troubleshooting/observability-troubleshooting.md)
- [Deployment指南](../deployment/observability-deployment.md)
