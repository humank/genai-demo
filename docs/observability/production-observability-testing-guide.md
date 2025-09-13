# 生產環境可觀測性測試指南

## 📋 目錄

1. [概述](#概述)
2. [測試目標](#測試目標)
3. [測試架構](#測試架構)
4. [核心測試場景](#核心測試場景)
5. [測試環境配置](#測試環境配置)
6. [測試執行計劃](#測試執行計劃)
7. [驗收標準](#驗收標準)
8. [故障排除](#故障排除)
9. [最佳實踐](#最佳實踐)

## 概述

本文件提供了在生產環境中實施完整可觀測性系統的詳細測試指南。基於 GenAI Demo 項目的可觀測性測試經驗，涵蓋日誌、指標、追蹤、健康檢查等全方位的可觀測性驗證。

### 適用範圍

- **目標環境**: AWS 生產環境
- **技術棧**: Spring Boot 3.x + Java 21 + AWS 可觀測性服務
- **測試類型**: 功能測試、性能測試、災難恢復測試
- **測試層級**: 單元測試、集成測試、端到端測試

## 測試目標

### 🎯 主要目標

1. **可觀測性完整性驗證**
   - 確保所有可觀測性組件正常運作
   - 驗證數據流完整性和準確性
   - 確保跨服務追蹤功能正常

2. **性能與可靠性驗證**
   - 驗證可觀測性系統在高負載下的穩定性
   - 確保可觀測性開銷在可接受範圍內 (< 5%)
   - 驗證系統恢復能力

3. **安全與合規性驗證**
   - 確保敏感數據正確遮罩
   - 驗證數據保留政策
   - 確保審計日誌完整性

4. **成本優化驗證**
   - 驗證採樣策略有效性
   - 確保資源使用優化
   - 驗證成本監控和告警

## 測試架構

### 🏗️ 可觀測性技術棧

```
┌─────────────────────────────────────────────────────────────┐
│                    生產環境可觀測性架構                        │
├─────────────────────────────────────────────────────────────┤
│  應用層                                                      │
│  ├── Spring Boot Actuator (健康檢查、指標)                   │
│  ├── Micrometer (指標收集)                                   │
│  ├── OpenTelemetry (分散式追蹤)                              │
│  └── Logback + MDC (結構化日誌)                              │
├─────────────────────────────────────────────────────────────┤
│  AWS 可觀測性服務                                            │
│  ├── CloudWatch (指標、日誌、告警)                           │
│  ├── X-Ray (分散式追蹤)                                      │
│  ├── CloudWatch Insights (日誌分析)                          │
│  └── CloudWatch Dashboards (可視化)                          │
├─────────────────────────────────────────────────────────────┤
│  第三方工具 (可選)                                           │
│  ├── Prometheus + Grafana                                   │
│  ├── ELK Stack (Elasticsearch, Logstash, Kibana)           │
│  └── Jaeger (追蹤可視化)                                     │
└─────────────────────────────────────────────────────────────┘
```

## 核心測試場景

### 🔍 1. 基礎可觀測性驗證

#### 1.1 結構化日誌測試

**測試目標**: 驗證結構化日誌記錄和 MDC 上下文傳播

**測試場景**:

```java
// 場景 1: MDC 上下文傳播測試
@Test
void shouldPropagateCorrelationIdThroughMDC() {
    // Given: 設置關聯 ID
    String correlationId = "prod-test-" + System.currentTimeMillis();
    MDC.put("correlationId", correlationId);
    MDC.put("userId", "test-user-123");
    MDC.put("requestId", "req-" + UUID.randomUUID());
    
    // When: 執行業務操作
    performBusinessOperation();
    
    // Then: 驗證日誌包含正確的上下文
    assertThat(getLogEntries())
        .allMatch(entry -> entry.contains(correlationId))
        .allMatch(entry -> entry.contains("test-user-123"));
}

// 場景 2: 跨服務日誌關聯測試
@Test
void shouldMaintainCorrelationAcrossServices() {
    // Given: 發起跨服務請求
    String correlationId = generateCorrelationId();
    
    // When: 調用多個微服務
    callOrderService(correlationId);
    callPaymentService(correlationId);
    callInventoryService(correlationId);
    
    // Then: 驗證所有服務日誌都包含相同的關聯 ID
    assertThat(getAllServiceLogs())
        .allMatch(log -> log.getCorrelationId().equals(correlationId));
}
```

**驗收標準**:

- ✅ 所有日誌條目包含必要的 MDC 上下文
- ✅ 關聯 ID 在整個請求生命週期中保持一致
- ✅ 日誌格式符合 JSON 結構化標準
- ✅ 敏感信息正確遮罩 (PII, 密碼等)

#### 1.2 指標收集測試

**測試目標**: 驗證業務指標和系統指標的準確收集

**測試場景**:

```java
// 場景 1: 業務指標測試
@Test
void shouldCollectBusinessMetrics() {
    // Given: 初始指標狀態
    double initialOrderCount = getMetricValue("orders.created.total");
    double initialRevenue = getMetricValue("revenue.total");
    
    // When: 執行業務操作
    createOrder(OrderBuilder.newOrder()
        .withAmount(BigDecimal.valueOf(100.00))
        .withCustomerId("customer-123")
        .build());
    
    // Then: 驗證指標更新
    await().atMost(30, SECONDS).untilAsserted(() -> {
        assertThat(getMetricValue("orders.created.total"))
            .isEqualTo(initialOrderCount + 1);
        assertThat(getMetricValue("revenue.total"))
            .isEqualTo(initialRevenue + 100.00);
    });
}

// 場景 2: 系統指標測試
@Test
void shouldCollectSystemMetrics() {
    // When: 系統運行一段時間
    generateSystemLoad();
    
    // Then: 驗證系統指標可用
    assertThat(getMetricValue("jvm.memory.used")).isGreaterThan(0);
    assertThat(getMetricValue("jvm.gc.pause")).isGreaterThan(0);
    assertThat(getMetricValue("http.server.requests")).isGreaterThan(0);
    assertThat(getMetricValue("database.connections.active")).isGreaterThan(0);
}
```

**驗收標準**:

- ✅ 業務指標準確反映實際操作
- ✅ 系統指標涵蓋 JVM、HTTP、數據庫等關鍵組件
- ✅ 指標標籤 (tags) 正確設置
- ✅ 指標數據在 Prometheus/CloudWatch 中可查詢

#### 1.3 分散式追蹤測試

**測試目標**: 驗證跨服務請求追蹤的完整性

**測試場景**:

```java
// 場景 1: 單一服務追蹤測試
@Test
void shouldCreateTraceForSingleService() {
    // Given: 啟用追蹤
    Span parentSpan = tracer.nextSpan().name("test-operation").start();
    
    try (Tracer.SpanInScope ws = tracer.withSpanInScope(parentSpan)) {
        // When: 執行業務操作
        String result = orderService.createOrder(createOrderRequest());
        
        // Then: 驗證 span 創建
        assertThat(result).isNotNull();
        assertThat(parentSpan.getTraceId()).isNotNull();
        assertThat(parentSpan.getSpanId()).isNotNull();
    } finally {
        parentSpan.end();
    }
}

// 場景 2: 跨服務追蹤測試
@Test
void shouldTraceAcrossMultipleServices() {
    // Given: 生成追蹤 ID
    String traceId = generateTraceId();
    
    // When: 執行跨服務操作
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Trace-ID", traceId);
    
    ResponseEntity<OrderResponse> response = restTemplate.exchange(
        "/api/orders", HttpMethod.POST, 
        new HttpEntity<>(createOrderRequest(), headers),
        OrderResponse.class
    );
    
    // Then: 驗證追蹤數據
    await().atMost(60, SECONDS).untilAsserted(() -> {
        List<Span> spans = getSpansForTrace(traceId);
        assertThat(spans).hasSizeGreaterThan(3); // Order, Payment, Inventory services
        assertThat(spans).allMatch(span -> span.getTraceId().equals(traceId));
    });
}
```

**驗收標準**:

- ✅ 每個請求都有唯一的追蹤 ID
- ✅ 跨服務調用保持追蹤上下文
- ✅ Span 包含必要的標籤和屬性
- ✅ 追蹤數據在 X-Ray/Jaeger 中可視化

### 🏥 2. 健康檢查與監控測試

#### 2.1 應用健康檢查測試

**測試目標**: 驗證應用和依賴服務的健康狀態監控

**測試場景**:

```java
// 場景 1: 基本健康檢查測試
@Test
void shouldReturnHealthyStatusWhenAllComponentsUp() {
    // When: 查詢健康狀態
    ResponseEntity<HealthResponse> response = restTemplate.getForEntity(
        "/actuator/health", HealthResponse.class);
    
    // Then: 驗證健康狀態
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getStatus()).isEqualTo("UP");
    assertThat(response.getBody().getComponents())
        .containsKeys("db", "diskSpace", "redis", "kafka");
}

// 場景 2: 依賴服務故障測試
@Test
void shouldReturnUnhealthyWhenDependencyDown() {
    // Given: 模擬數據庫故障
    simulateDatabaseFailure();
    
    // When: 查詢健康狀態
    ResponseEntity<HealthResponse> response = restTemplate.getForEntity(
        "/actuator/health", HealthResponse.class);
    
    // Then: 驗證不健康狀態
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(response.getBody().getStatus()).isEqualTo("DOWN");
    assertThat(response.getBody().getComponents().get("db").getStatus())
        .isEqualTo("DOWN");
}
```

#### 2.2 Kubernetes 探針測試

**測試目標**: 驗證 Kubernetes liveness 和 readiness 探針

**測試場景**:

```java
// 場景 1: Liveness 探針測試
@Test
void shouldRespondToLivenessProbe() {
    // When: 查詢 liveness 探針
    ResponseEntity<String> response = restTemplate.getForEntity(
        "/actuator/health/liveness", String.class);
    
    // Then: 驗證響應
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"status\":\"UP\"");
}

// 場景 2: Readiness 探針測試
@Test
void shouldRespondToReadinessProbe() {
    // When: 查詢 readiness 探針
    ResponseEntity<String> response = restTemplate.getForEntity(
        "/actuator/health/readiness", String.class);
    
    // Then: 驗證響應
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"status\":\"UP\"");
}
```

**驗收標準**:

- ✅ 健康檢查響應時間 < 5 秒
- ✅ Liveness 探針響應時間 < 2 秒
- ✅ Readiness 探針響應時間 < 3 秒
- ✅ 依賴服務故障時正確報告不健康狀態

### 🚀 3. 性能與負載測試

#### 3.1 可觀測性開銷測試

**測試目標**: 驗證可觀測性系統對應用性能的影響

**測試場景**:

```java
// 場景 1: 基準性能測試
@Test
void shouldMeasureObservabilityOverhead() {
    // Given: 禁用可觀測性
    disableObservability();
    
    // When: 執行基準測試
    long baselineLatency = measureAverageLatency(1000);
    double baselineThroughput = measureThroughput(60);
    
    // Given: 啟用可觀測性
    enableObservability();
    
    // When: 執行相同測試
    long observabilityLatency = measureAverageLatency(1000);
    double observabilityThroughput = measureThroughput(60);
    
    // Then: 驗證性能影響在可接受範圍內
    double latencyOverhead = (observabilityLatency - baselineLatency) / (double) baselineLatency * 100;
    double throughputImpact = (baselineThroughput - observabilityThroughput) / baselineThroughput * 100;
    
    assertThat(latencyOverhead).isLessThan(5.0); // < 5% 延遲增加
    assertThat(throughputImpact).isLessThan(3.0); // < 3% 吞吐量下降
}

// 場景 2: 高負載下的穩定性測試
@Test
void shouldMaintainStabilityUnderHighLoad() {
    // Given: 配置高負載測試
    int concurrentUsers = 100;
    Duration testDuration = Duration.ofMinutes(10);
    
    // When: 執行負載測試
    LoadTestResult result = executeLoadTest(concurrentUsers, testDuration);
    
    // Then: 驗證系統穩定性
    assertThat(result.getErrorRate()).isLessThan(0.1); // < 0.1% 錯誤率
    assertThat(result.getP95Latency()).isLessThan(Duration.ofSeconds(2)); // P95 < 2s
    assertThat(result.getMemoryLeaks()).isEmpty(); // 無記憶體洩漏
    
    // 驗證可觀測性數據完整性
    assertThat(getMetricsCompleteness()).isGreaterThan(0.99); // > 99% 數據完整性
    assertThat(getTraceCompleteness()).isGreaterThan(0.95); // > 95% 追蹤完整性
}
```

#### 3.2 採樣策略測試

**測試目標**: 驗證追蹤採樣策略的有效性

**測試場景**:

```java
// 場景 1: 採樣率驗證測試
@Test
void shouldRespectSamplingConfiguration() {
    // Given: 設置 10% 採樣率
    setSamplingRate(0.1);
    
    // When: 生成大量請求
    int totalRequests = 10000;
    for (int i = 0; i < totalRequests; i++) {
        makeRequest("/api/test");
    }
    
    // Then: 驗證採樣率
    await().atMost(2, MINUTES).untilAsserted(() -> {
        long sampledTraces = countSampledTraces();
        double actualSamplingRate = sampledTraces / (double) totalRequests;
        
        assertThat(actualSamplingRate)
            .isBetween(0.08, 0.12); // 允許 ±2% 誤差
    });
}

// 場景 2: 智能採樣測試
@Test
void shouldApplyIntelligentSampling() {
    // Given: 配置智能採樣 (錯誤請求 100% 採樣，正常請求 1% 採樣)
    configureIntelligentSampling();
    
    // When: 生成混合請求
    generateNormalRequests(1000);
    generateErrorRequests(10);
    
    // Then: 驗證採樣策略
    await().atMost(1, MINUTES).untilAsserted(() -> {
        assertThat(getErrorTraceSamplingRate()).isEqualTo(1.0); // 100% 錯誤採樣
        assertThat(getNormalTraceSamplingRate()).isBetween(0.005, 0.015); // ~1% 正常採樣
    });
}
```

### 🔒 4. 安全與合規性測試

#### 4.1 敏感數據遮罩測試

**測試目標**: 驗證敏感信息在日誌和追蹤中正確遮罩

**測試場景**:

```java
// 場景 1: PII 數據遮罩測試
@Test
void shouldMaskPiiDataInLogs() {
    // Given: 包含 PII 的請求
    CreateUserRequest request = CreateUserRequest.builder()
        .email("user@example.com")
        .phone("0912345678")
        .creditCard("4111111111111111")
        .ssn("123-45-6789")
        .build();
    
    // When: 處理請求
    userService.createUser(request);
    
    // Then: 驗證日誌中 PII 被遮罩
    List<LogEntry> logs = getRecentLogEntries();
    assertThat(logs).allMatch(log -> {
        String content = log.getMessage();
        return !content.contains("user@example.com") &&
               !content.contains("0912345678") &&
               !content.contains("4111111111111111") &&
               !content.contains("123-45-6789") &&
               content.contains("***@***.com") && // 遮罩後的格式
               content.contains("091****678");
    });
}

// 場景 2: 追蹤數據安全測試
@Test
void shouldNotExposeSecretsInTraces() {
    // Given: 包含敏感配置的操作
    String apiKey = "secret-api-key-12345";
    String password = "super-secret-password";
    
    // When: 執行需要認證的操作
    authenticatedService.performOperation(apiKey, password);
    
    // Then: 驗證追蹤數據不包含敏感信息
    await().atMost(30, SECONDS).untilAsserted(() -> {
        List<Span> spans = getRecentSpans();
        assertThat(spans).allMatch(span -> {
            Map<String, String> tags = span.getTags();
            return !tags.values().stream().anyMatch(value -> 
                value.contains(apiKey) || value.contains(password));
        });
    });
}
```

#### 4.2 審計日誌測試

**測試目標**: 驗證關鍵操作的審計日誌記錄

**測試場景**:

```java
// 場景 1: 用戶操作審計測試
@Test
void shouldLogUserOperationsForAudit() {
    // Given: 用戶執行關鍵操作
    String userId = "user-123";
    String operation = "DELETE_ORDER";
    String orderId = "order-456";
    
    // When: 執行刪除操作
    orderService.deleteOrder(orderId, userId);
    
    // Then: 驗證審計日誌
    await().atMost(10, SECONDS).untilAsserted(() -> {
        List<AuditLogEntry> auditLogs = getAuditLogs();
        assertThat(auditLogs).anyMatch(entry ->
            entry.getUserId().equals(userId) &&
            entry.getOperation().equals(operation) &&
            entry.getResourceId().equals(orderId) &&
            entry.getTimestamp() != null &&
            entry.getIpAddress() != null
        );
    });
}
```

### 💰 5. 成本優化測試

#### 5.1 資源使用監控測試

**測試目標**: 驗證可觀測性資源使用監控和優化

**測試場景**:

```java
// 場景 1: CloudWatch 成本監控測試
@Test
void shouldMonitorCloudWatchCosts() {
    // Given: 運行一段時間收集數據
    Duration monitoringPeriod = Duration.ofHours(1);
    runApplicationUnderLoad(monitoringPeriod);
    
    // When: 查詢成本指標
    CloudWatchCostMetrics costs = cloudWatchCostService.getCostMetrics(monitoringPeriod);
    
    // Then: 驗證成本在預期範圍內
    assertThat(costs.getLogIngestionCost()).isLessThan(10.0); // < $10/hour
    assertThat(costs.getMetricsCost()).isLessThan(5.0); // < $5/hour
    assertThat(costs.getTracingCost()).isLessThan(15.0); // < $15/hour
    
    // 驗證成本告警配置
    assertThat(costs.hasAlertConfigured()).isTrue();
    assertThat(costs.getAlertThreshold()).isLessThan(50.0); // < $50/hour 告警
}

// 場景 2: 數據保留策略測試
@Test
void shouldApplyDataRetentionPolicies() {
    // Given: 配置數據保留策略
    configureRetentionPolicies(
        logs: Duration.ofDays(30),
        metrics: Duration.ofDays(90),
        traces: Duration.ofDays(7)
    );
    
    // When: 等待保留策略生效
    waitForRetentionPolicyApplication();
    
    // Then: 驗證舊數據被清理
    assertThat(getLogDataAge()).isLessThanOrEqualTo(Duration.ofDays(30));
    assertThat(getMetricDataAge()).isLessThanOrEqualTo(Duration.ofDays(90));
    assertThat(getTraceDataAge()).isLessThanOrEqualTo(Duration.ofDays(7));
}
```

### 🌐 6. 多環境與災難恢復測試

#### 6.1 多區域部署測試

**測試目標**: 驗證跨區域可觀測性數據同步和故障轉移

**測試場景**:

```java
// 場景 1: 跨區域數據同步測試
@Test
void shouldSynchronizeObservabilityDataAcrossRegions() {
    // Given: 多區域部署
    List<String> regions = Arrays.asList("us-east-1", "us-west-2", "eu-west-1");
    
    // When: 在主區域生成數據
    String correlationId = generateDataInPrimaryRegion();
    
    // Then: 驗證數據在所有區域可見
    await().atMost(5, MINUTES).untilAsserted(() -> {
        for (String region : regions) {
            assertThat(isDataAvailableInRegion(correlationId, region)).isTrue();
        }
    });
}

// 場景 2: 區域故障轉移測試
@Test
void shouldFailoverObservabilityServicesGracefully() {
    // Given: 主區域正常運行
    String primaryRegion = "us-east-1";
    String secondaryRegion = "us-west-2";
    
    // When: 模擬主區域故障
    simulateRegionFailure(primaryRegion);
    
    // Then: 驗證故障轉移
    await().atMost(2, MINUTES).untilAsserted(() -> {
        assertThat(isObservabilityServiceActive(secondaryRegion)).isTrue();
        assertThat(getActiveRegion()).isEqualTo(secondaryRegion);
    });
    
    // 驗證數據完整性
    assertThat(getDataLossDuringFailover()).isLessThan(0.01); // < 1% 數據丟失
}
```

#### 6.2 災難恢復測試

**測試目標**: 驗證可觀測性系統的災難恢復能力

**測試場景**:

```java
// 場景 1: 完整災難恢復測試
@Test
void shouldRecoverFromCompleteSystemFailure() {
    // Given: 系統正常運行
    generateBaselineData();
    
    // When: 模擬完整系統故障
    simulateCompleteSystemFailure();
    
    // 執行災難恢復程序
    executeDisasterRecoveryProcedure();
    
    // Then: 驗證系統恢復
    await().atMost(30, MINUTES).untilAsserted(() -> {
        assertThat(isSystemFullyRecovered()).isTrue();
        assertThat(getDataIntegrityScore()).isGreaterThan(0.95); // > 95% 數據完整性
        assertThat(getRecoveryTimeObjective()).isLessThan(Duration.ofMinutes(30)); // RTO < 30min
    });
}

// 場景 2: 部分服務故障恢復測試
@Test
void shouldRecoverFromPartialServiceFailure() {
    // Given: 模擬部分服務故障
    simulateServiceFailure("tracing-service");
    
    // When: 系統繼續運行
    continueOperationsWithDegradedService();
    
    // Then: 驗證優雅降級
    assertThat(isLoggingServiceActive()).isTrue();
    assertThat(isMetricsServiceActive()).isTrue();
    assertThat(isTracingServiceActive()).isFalse();
    
    // 驗證服務恢復
    restoreService("tracing-service");
    await().atMost(5, MINUTES).untilAsserted(() -> {
        assertThat(isTracingServiceActive()).isTrue();
        assertThat(getServiceHealthScore()).isEqualTo(1.0); // 100% 健康
    });
}
```

### 🔄 7. CI/CD 集成測試

#### 7.1 部署管道可觀測性測試

**測試目標**: 驗證 CI/CD 管道中的可觀測性集成

**測試場景**:

```java
// 場景 1: 部署指標測試
@Test
void shouldCollectDeploymentMetrics() {
    // Given: 觸發部署
    String deploymentId = triggerDeployment();
    
    // When: 部署執行
    waitForDeploymentCompletion(deploymentId);
    
    // Then: 驗證部署指標
    DeploymentMetrics metrics = getDeploymentMetrics(deploymentId);
    assertThat(metrics.getDeploymentDuration()).isLessThan(Duration.ofMinutes(15));
    assertThat(metrics.getSuccessRate()).isEqualTo(1.0);
    assertThat(metrics.getRollbackCount()).isEqualTo(0);
}

// 場景 2: 品質門檻測試
@Test
void shouldEnforceQualityGatesInPipeline() {
    // Given: 配置品質門檻
    configureQualityGates(
        errorRate: 0.01,        // < 1% 錯誤率
        responseTime: Duration.ofSeconds(2), // < 2s 響應時間
        availability: 0.999     // > 99.9% 可用性
    );
    
    // When: 執行部署驗證
    QualityGateResult result = executeQualityGateValidation();
    
    // Then: 驗證品質門檻
    assertThat(result.getErrorRate()).isLessThan(0.01);
    assertThat(result.getAverageResponseTime()).isLessThan(Duration.ofSeconds(2));
    assertThat(result.getAvailability()).isGreaterThan(0.999);
    assertThat(result.isPassed()).isTrue();
}
```

## 測試環境配置

### 🛠️ 生產環境配置

#### AWS 服務配置

```yaml
# CloudWatch 配置
cloudwatch:
  region: us-east-1
  log-groups:
    - name: /aws/lambda/genai-demo
      retention-days: 30
    - name: /aws/ecs/genai-demo
      retention-days: 30
  metrics:
    namespace: GenAIDemo/Production
    dimensions:
      Environment: production
      Service: genai-demo
  alarms:
    - name: HighErrorRate
      threshold: 0.05
      comparison: GreaterThanThreshold
    - name: HighLatency
      threshold: 2000
      comparison: GreaterThanThreshold

# X-Ray 配置
xray:
  tracing-config:
    sampling-rate: 0.1
    reservoir-size: 1
  service-map:
    enabled: true
  annotations:
    - service
    - environment
    - version

# Prometheus 配置 (可選)
prometheus:
  scrape-configs:
    - job_name: genai-demo
      static_configs:
        - targets: ['localhost:8080']
      metrics_path: /actuator/prometheus
      scrape_interval: 30s
```

#### Spring Boot 配置

```yaml
# application-production.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
      show-components: always
      probes:
        enabled: true
    metrics:
      enabled: true
    prometheus:
      enabled: true
  metrics:
    export:
      cloudwatch:
        enabled: true
        namespace: GenAIDemo/Production
        batch-size: 20
      prometheus:
        enabled: true
    tags:
      environment: production
      service: genai-demo
      version: ${app.version:unknown}

# 日誌配置
logging:
  level:
    solid.humank.genaidemo: INFO
    org.springframework.web: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level [%X{correlationId:-}] %logger{36} - %msg%n"
  appender:
    cloudwatch:
      enabled: true
      log-group: /aws/ecs/genai-demo
      log-stream: ${HOSTNAME:localhost}

# 追蹤配置
tracing:
  enabled: true
  sampling:
    probability: 0.1
  zipkin:
    enabled: false
  jaeger:
    enabled: false
  xray:
    enabled: true
```

### 🧪 測試數據準備

#### 測試數據生成器

```java
@Component
public class ObservabilityTestDataGenerator {
    
    public void generateBusinessOperations(int count) {
        for (int i = 0; i < count; i++) {
            // 生成訂單
            createTestOrder();
            
            // 生成支付
            processTestPayment();
            
            // 生成庫存操作
            updateTestInventory();
            
            // 隨機延遲
            randomDelay();
        }
    }
    
    public void generateErrorScenarios() {
        // 生成 4xx 錯誤
        generate4xxErrors();
        
        // 生成 5xx 錯誤
        generate5xxErrors();
        
        // 生成超時錯誤
        generateTimeoutErrors();
        
        // 生成數據庫連接錯誤
        generateDatabaseErrors();
    }
    
    public void generateHighLoadScenario(Duration duration) {
        long endTime = System.currentTimeMillis() + duration.toMillis();
        
        while (System.currentTimeMillis() < endTime) {
            CompletableFuture.runAsync(this::generateBusinessOperations);
            Thread.sleep(100); // 控制負載
        }
    }
}
```

## 測試執行計劃

### 📅 測試階段規劃

#### 階段 1: 基礎驗證 (1-2 天)

**目標**: 驗證基本可觀測性功能

**測試內容**:

- ✅ 應用啟動和健康檢查
- ✅ 基本日誌記錄和格式化
- ✅ 基本指標收集
- ✅ 簡單追蹤功能

**成功標準**:

- 所有健康檢查端點正常響應
- 日誌格式符合 JSON 標準
- 基本指標在 CloudWatch 中可見
- 單一服務追蹤正常工作

#### 階段 2: 集成驗證 (3-5 天)

**目標**: 驗證跨服務可觀測性集成

**測試內容**:

- ✅ 跨服務追蹤
- ✅ 關聯 ID 傳播
- ✅ 業務指標準確性
- ✅ 錯誤處理和監控

**成功標準**:

- 跨服務追蹤完整性 > 95%
- 關聯 ID 傳播成功率 > 99%
- 業務指標與實際操作一致
- 錯誤監控和告警正常

#### 階段 3: 性能驗證 (2-3 天)

**目標**: 驗證可觀測性系統性能影響

**測試內容**:

- ✅ 性能基準測試
- ✅ 負載測試
- ✅ 採樣策略驗證
- ✅ 資源使用監控

**成功標準**:

- 可觀測性開銷 < 5%
- 高負載下系統穩定
- 採樣策略有效
- 資源使用在預期範圍內

#### 階段 4: 安全與合規驗證 (2-3 天)

**目標**: 驗證安全和合規要求

**測試內容**:

- ✅ 敏感數據遮罩
- ✅ 審計日誌完整性
- ✅ 數據保留策略
- ✅ 訪問控制

**成功標準**:

- PII 數據 100% 遮罩
- 審計日誌完整記錄
- 數據保留策略生效
- 訪問控制正確配置

#### 階段 5: 災難恢復驗證 (3-5 天)

**目標**: 驗證災難恢復能力

**測試內容**:

- ✅ 多區域故障轉移
- ✅ 數據備份和恢復
- ✅ 服務降級處理
- ✅ 完整災難恢復

**成功標準**:

- RTO < 30 分鐘
- RPO < 5 分鐘
- 數據完整性 > 95%
- 服務降級正常

### 🎯 測試執行檢查清單

#### 測試前準備

- [ ] **環境準備**
  - [ ] AWS 帳戶和權限配置
  - [ ] CloudWatch、X-Ray 服務啟用
  - [ ] 測試數據準備
  - [ ] 監控儀表板設置

- [ ] **工具準備**
  - [ ] 負載測試工具 (JMeter/Gatling)
  - [ ] 監控工具 (Grafana/CloudWatch Dashboard)
  - [ ] 日誌分析工具 (CloudWatch Insights)
  - [ ] 追蹤分析工具 (X-Ray Console)

- [ ] **團隊準備**
  - [ ] 測試團隊培訓
  - [ ] 角色和責任分配
  - [ ] 溝通渠道建立
  - [ ] 緊急聯絡方式

#### 測試執行

- [ ] **每日檢查**
  - [ ] 測試環境健康狀態
  - [ ] 測試數據完整性
  - [ ] 測試結果記錄
  - [ ] 問題追蹤和解決

- [ ] **每週回顧**
  - [ ] 測試進度評估
  - [ ] 風險識別和緩解
  - [ ] 測試計劃調整
  - [ ] 利害關係人溝通

#### 測試後清理

- [ ] **數據清理**
  - [ ] 測試數據清除
  - [ ] 臨時資源釋放
  - [ ] 成本分析和優化
  - [ ] 環境重置

- [ ] **文檔整理**
  - [ ] 測試報告編寫
  - [ ] 問題和解決方案記錄
  - [ ] 最佳實踐總結
  - [ ] 知識轉移

## 驗收標準

### 🎯 功能性驗收標準

#### 日誌系統

- ✅ **結構化日誌**: 100% 的日誌條目符合 JSON 格式
- ✅ **MDC 傳播**: 關聯 ID 在整個請求生命週期中保持一致
- ✅ **日誌等級**: 支援動態調整日誌等級
- ✅ **敏感數據**: PII 和機密信息 100% 遮罩

#### 指標系統

- ✅ **業務指標**: 準確反映實際業務操作
- ✅ **系統指標**: 涵蓋 JVM、HTTP、數據庫等關鍵組件
- ✅ **自定義指標**: 支援業務特定指標定義
- ✅ **指標標籤**: 正確設置維度標籤

#### 追蹤系統

- ✅ **跨服務追蹤**: 完整性 > 95%
- ✅ **追蹤上下文**: 正確傳播追蹤上下文
- ✅ **採樣策略**: 採樣率控制在配置範圍內
- ✅ **追蹤可視化**: 在 X-Ray/Jaeger 中正確顯示

#### 健康檢查

- ✅ **響應時間**: 健康檢查 < 5s，探針 < 3s
- ✅ **依賴檢查**: 正確檢測依賴服務狀態
- ✅ **故障處理**: 依賴故障時正確報告狀態
- ✅ **Kubernetes 集成**: 探針與 K8s 正確集成

### 📊 性能驗收標準

#### 系統性能

- ✅ **延遲影響**: 可觀測性開銷 < 5%
- ✅ **吞吐量影響**: 吞吐量下降 < 3%
- ✅ **記憶體使用**: 記憶體增加 < 10%
- ✅ **CPU 使用**: CPU 增加 < 5%

#### 可擴展性

- ✅ **高負載穩定性**: 100 併發用戶下穩定運行
- ✅ **數據完整性**: 高負載下數據完整性 > 99%
- ✅ **錯誤率**: 系統錯誤率 < 0.1%
- ✅ **恢復時間**: 負載後恢復時間 < 5 分鐘

### 🔒 安全驗收標準

#### 數據保護

- ✅ **PII 遮罩**: 個人識別信息 100% 遮罩
- ✅ **機密信息**: API 金鑰、密碼等不出現在日誌中
- ✅ **數據傳輸**: 所有數據傳輸使用 TLS 加密
- ✅ **訪問控制**: 基於角色的訪問控制正確配置

#### 合規性

- ✅ **審計日誌**: 關鍵操作 100% 記錄
- ✅ **數據保留**: 按政策自動清理過期數據
- ✅ **法規遵循**: 符合 GDPR、SOX 等法規要求
- ✅ **數據主權**: 數據存儲符合地區法規

### 💰 成本驗收標準

#### 成本控制

- ✅ **CloudWatch 成本**: < $100/月 (基於預期負載)
- ✅ **X-Ray 成本**: < $50/月
- ✅ **存儲成本**: < $30/月
- ✅ **總成本**: 可觀測性總成本 < 運營成本的 5%

#### 成本優化

- ✅ **採樣策略**: 有效降低追蹤成本
- ✅ **數據保留**: 自動清理降低存儲成本
- ✅ **告警配置**: 成本異常及時告警
- ✅ **使用監控**: 定期審查和優化使用

## 故障排除

### 🚨 常見問題與解決方案

#### 日誌問題

**問題**: 日誌格式不正確

```
解決方案:
1. 檢查 logback-spring.xml 配置
2. 驗證 JSON 編碼器設置
3. 確認 MDC 配置正確
4. 測試日誌輸出格式
```

**問題**: 關聯 ID 丟失

```
解決方案:
1. 檢查 MDC 過濾器配置
2. 驗證異步處理中的上下文傳播
3. 確認跨服務調用中的標頭傳遞
4. 檢查線程池配置
```

#### 指標問題

**問題**: 指標數據缺失

```
解決方案:
1. 檢查 Micrometer 配置
2. 驗證 CloudWatch 權限
3. 確認指標註冊正確
4. 檢查網路連接
```

**問題**: 指標數據不準確

```
解決方案:
1. 驗證指標計算邏輯
2. 檢查標籤設置
3. 確認時間同步
4. 檢查採樣配置
```

#### 追蹤問題

**問題**: 追蹤數據不完整

```
解決方案:
1. 檢查 OpenTelemetry 配置
2. 驗證採樣率設置
3. 確認 X-Ray 權限
4. 檢查網路連接和防火牆
```

**問題**: 跨服務追蹤中斷

```
解決方案:
1. 檢查追蹤上下文傳播
2. 驗證 HTTP 標頭配置
3. 確認服務間通信配置
4. 檢查追蹤 ID 格式
```

### 🔧 診斷工具

#### 日誌診斷

```bash
# 檢查日誌格式
curl -s http://localhost:8080/actuator/loggers | jq .

# 測試 MDC 功能
curl -H "X-Correlation-ID: test-123" http://localhost:8080/api/test

# 查看 CloudWatch 日誌
aws logs describe-log-groups --log-group-name-prefix "/aws/ecs/genai-demo"
```

#### 指標診斷

```bash
# 檢查 Prometheus 端點
curl -s http://localhost:8080/actuator/prometheus | grep genai_demo

# 查看 CloudWatch 指標
aws cloudwatch list-metrics --namespace "GenAIDemo/Production"

# 測試自定義指標
curl -X POST http://localhost:8080/api/test/metrics
```

#### 追蹤診斷

```bash
# 檢查 X-Ray 服務地圖
aws xray get-service-graph --start-time 2024-01-01T00:00:00Z --end-time 2024-01-01T23:59:59Z

# 查看追蹤摘要
aws xray get-trace-summaries --time-range-type TimeRangeByStartTime --start-time 2024-01-01T00:00:00Z --end-time 2024-01-01T01:00:00Z
```

## 最佳實踐

### 🏆 開發最佳實踐

#### 日誌最佳實踐

1. **使用結構化日誌**: 始終使用 JSON 格式
2. **合理的日誌等級**: 生產環境使用 INFO 以上等級
3. **避免敏感信息**: 永遠不要記錄密碼、API 金鑰等
4. **使用 MDC**: 為每個請求設置關聯 ID
5. **異常處理**: 記錄完整的異常堆疊追蹤

#### 指標最佳實踐

1. **業務指標優先**: 關注業務價值指標
2. **合理的維度**: 避免高基數標籤
3. **一致的命名**: 使用統一的指標命名規範
4. **適當的類型**: 選擇正確的指標類型 (Counter, Gauge, Timer)
5. **定期清理**: 移除不再使用的指標

#### 追蹤最佳實踐

1. **智能採樣**: 錯誤請求 100% 採樣，正常請求低採樣
2. **有意義的 Span**: 為重要操作創建 Span
3. **正確的標籤**: 添加有助於分析的標籤
4. **上下文傳播**: 確保跨服務上下文正確傳播
5. **性能考量**: 避免追蹤對性能造成顯著影響

### 🎯 運維最佳實踐

#### 監控策略

1. **分層監控**: 基礎設施、應用、業務三層監控
2. **主動告警**: 基於 SLI/SLO 設置告警
3. **告警疲勞**: 避免過多無意義告警
4. **根因分析**: 建立問題根因分析流程
5. **持續改進**: 定期回顧和優化監控策略

#### 成本管理

1. **定期審查**: 每月審查可觀測性成本
2. **採樣優化**: 根據價值調整採樣策略
3. **數據生命週期**: 實施自動數據清理
4. **資源標籤**: 使用標籤進行成本分配
5. **預算告警**: 設置成本預算和告警

#### 安全管理

1. **最小權限**: 遵循最小權限原則
2. **定期審計**: 定期審計訪問權限
3. **數據分類**: 對可觀測性數據進行分類
4. **合規檢查**: 定期進行合規性檢查
5. **事件響應**: 建立安全事件響應流程

---

## 📚 參考資源

### 技術文檔

- [Spring Boot Actuator 官方文檔](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Micrometer 官方文檔](https://micrometer.io/docs)
- [OpenTelemetry Java 文檔](https://opentelemetry.io/docs/instrumentation/java/)
- [AWS CloudWatch 文檔](https://docs.aws.amazon.com/cloudwatch/)
- [AWS X-Ray 文檔](https://docs.aws.amazon.com/xray/)

### 最佳實踐指南

- [Google SRE Book - Monitoring](https://sre.google/sre-book/monitoring-distributed-systems/)
- [AWS Well-Architected Framework - Observability](https://docs.aws.amazon.com/wellarchitected/latest/framework/ops_observability.html)
- [CNCF Observability Whitepaper](https://github.com/cncf/tag-observability/blob/main/whitepaper.md)

### 工具和平台

- [Prometheus](https://prometheus.io/)
- [Grafana](https://grafana.com/)
- [Jaeger](https://www.jaegertracing.io/)
- [ELK Stack](https://www.elastic.co/elastic-stack/)

---

**文檔版本**: 1.0  
**最後更新**: 2025年9月  
**維護者**: GenAI Demo 開發團隊  
**審核者**: 架構團隊、SRE 團隊

---

## 🌟 業界主流可觀測性測試實踐

### 📊 **實際業界做法分析**

您提出了一個很重要的問題！實務上，業界測試可觀測性系統**很少**直接用 Java 程序來測試。以下是業界主流的實際做法：

### 🛠️ **1. 基於工具的測試方法**

#### **Synthetic Monitoring (合成監控)**

```yaml
# DataDog Synthetic Tests
synthetic_tests:
  - name: "API Health Check"
    type: api
    request:
      url: "https://api.example.com/health"
      method: GET
    assertions:
      - type: statusCode
        operator: is
        value: 200
      - type: responseTime
        operator: lessThan
        value: 2000
    locations: ["aws:us-east-1", "aws:eu-west-1"]
    frequency: 300 # 5 minutes

  - name: "User Journey Test"
    type: browser
    steps:
      - type: click
        element: "#login-button"
      - type: type
        element: "#username"
        text: "test@example.com"
    locations: ["aws:us-east-1"]
    frequency: 900 # 15 minutes
```

#### **Chaos Engineering 測試**

```yaml
# Chaos Monkey / Litmus 配置
chaos_experiments:
  - name: "pod-delete"
    spec:
      components:
        env:
          - name: TOTAL_CHAOS_DURATION
            value: "60"
          - name: CHAOS_INTERVAL
            value: "10"
      probe:
        - name: "check-observability-metrics"
          type: "httpProbe"
          httpProbe/inputs:
            url: "http://prometheus:9090/api/v1/query"
            method:
              get:
                criteria: "=="
                responseCode: "200"
```

### 🔧 **2. 基於腳本的測試方法**

#### **Bash/Shell 腳本測試**

```bash
#!/bin/bash
# observability_health_check.sh

# 檢查 Prometheus 指標
check_prometheus_metrics() {
    echo "Checking Prometheus metrics..."
    
    # 檢查應用指標
    APP_METRICS=$(curl -s "http://prometheus:9090/api/v1/query?query=up{job=\"app\"}" | jq -r '.data.result[0].value[1]')
    if [ "$APP_METRICS" != "1" ]; then
        echo "❌ Application metrics not available"
        exit 1
    fi
    
    # 檢查錯誤率
    ERROR_RATE=$(curl -s "http://prometheus:9090/api/v1/query?query=rate(http_requests_total{status=~\"5..\"}[5m])" | jq -r '.data.result[0].value[1]')
    if (( $(echo "$ERROR_RATE > 0.01" | bc -l) )); then
        echo "❌ Error rate too high: $ERROR_RATE"
        exit 1
    fi
    
    echo "✅ Prometheus metrics healthy"
}

# 檢查日誌完整性
check_log_completeness() {
    echo "Checking log completeness..."
    
    # 檢查最近 5 分鐘的日誌
    LOG_COUNT=$(curl -s -G "http://elasticsearch:9200/logs-*/_search" \
        --data-urlencode 'q=@timestamp:[now-5m TO now]' | jq '.hits.total.value')
    
    if [ "$LOG_COUNT" -lt 100 ]; then
        echo "❌ Insufficient logs in last 5 minutes: $LOG_COUNT"
        exit 1
    fi
    
    echo "✅ Log completeness verified: $LOG_COUNT logs"
}

# 檢查追蹤數據
check_tracing_data() {
    echo "Checking tracing data..."
    
    # 檢查 Jaeger 中的追蹤
    TRACE_COUNT=$(curl -s "http://jaeger:16686/api/traces?service=app&lookback=5m" | jq '.data | length')
    
    if [ "$TRACE_COUNT" -lt 10 ]; then
        echo "❌ Insufficient traces: $TRACE_COUNT"
        exit 1
    fi
    
    echo "✅ Tracing data verified: $TRACE_COUNT traces"
}

# 主執行流程
main() {
    check_prometheus_metrics
    check_log_completeness  
    check_tracing_data
    echo "🎉 All observability checks passed!"
}

main "$@"
```

#### **Python 腳本測試**

```python
#!/usr/bin/env python3
# observability_validator.py

import requests
import json
import time
from datetime import datetime, timedelta

class ObservabilityValidator:
    def __init__(self, config):
        self.prometheus_url = config['prometheus_url']
        self.elasticsearch_url = config['elasticsearch_url']
        self.jaeger_url = config['jaeger_url']
    
    def validate_sli_slo(self):
        """驗證 SLI/SLO 指標"""
        print("🔍 Validating SLI/SLO metrics...")
        
        # 檢查可用性 SLI (目標: 99.9%)
        availability_query = 'avg_over_time(up{job="app"}[5m])'
        availability = self._query_prometheus(availability_query)
        
        if availability < 0.999:
            raise Exception(f"❌ Availability SLI failed: {availability:.4f} < 0.999")
        
        # 檢查延遲 SLI (目標: P95 < 2s)
        latency_query = 'histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))'
        p95_latency = self._query_prometheus(latency_query)
        
        if p95_latency > 2.0:
            raise Exception(f"❌ Latency SLI failed: P95={p95_latency:.2f}s > 2s")
        
        # 檢查錯誤率 SLI (目標: < 0.1%)
        error_rate_query = 'rate(http_requests_total{status=~"5.."}[5m]) / rate(http_requests_total[5m])'
        error_rate = self._query_prometheus(error_rate_query)
        
        if error_rate > 0.001:
            raise Exception(f"❌ Error rate SLI failed: {error_rate:.4f} > 0.001")
        
        print("✅ All SLI/SLO metrics within targets")
    
    def validate_log_pipeline(self):
        """驗證日誌管道完整性"""
        print("🔍 Validating log pipeline...")
        
        # 生成測試日誌
        test_correlation_id = f"test-{int(time.time())}"
        self._generate_test_logs(test_correlation_id)
        
        # 等待日誌處理
        time.sleep(30)
        
        # 驗證日誌是否到達 Elasticsearch
        query = {
            "query": {
                "bool": {
                    "must": [
                        {"match": {"correlation_id": test_correlation_id}},
                        {"range": {"@timestamp": {"gte": "now-2m"}}}
                    ]
                }
            }
        }
        
        response = requests.post(
            f"{self.elasticsearch_url}/logs-*/_search",
            json=query
        )
        
        hits = response.json()['hits']['total']['value']
        if hits == 0:
            raise Exception(f"❌ Test logs not found in Elasticsearch")
        
        print(f"✅ Log pipeline validated: {hits} test logs processed")
    
    def validate_alerting_rules(self):
        """驗證告警規則"""
        print("🔍 Validating alerting rules...")
        
        # 檢查 Prometheus 告警規則
        response = requests.get(f"{self.prometheus_url}/api/v1/rules")
        rules = response.json()['data']['groups']
        
        required_alerts = [
            'HighErrorRate',
            'HighLatency', 
            'ServiceDown',
            'DiskSpaceLow'
        ]
        
        active_alerts = []
        for group in rules:
            for rule in group['rules']:
                if rule['type'] == 'alerting':
                    active_alerts.append(rule['name'])
        
        missing_alerts = set(required_alerts) - set(active_alerts)
        if missing_alerts:
            raise Exception(f"❌ Missing alert rules: {missing_alerts}")
        
        print(f"✅ All required alert rules configured: {len(active_alerts)} rules")
    
    def _query_prometheus(self, query):
        """查詢 Prometheus 指標"""
        response = requests.get(
            f"{self.prometheus_url}/api/v1/query",
            params={'query': query}
        )
        result = response.json()['data']['result']
        return float(result[0]['value'][1]) if result else 0.0
    
    def _generate_test_logs(self, correlation_id):
        """生成測試日誌"""
        # 調用應用 API 生成日誌
        requests.get(
            "http://app:8080/api/test/logs",
            headers={"X-Correlation-ID": correlation_id}
        )

# 使用範例
if __name__ == "__main__":
    config = {
        'prometheus_url': 'http://prometheus:9090',
        'elasticsearch_url': 'http://elasticsearch:9200',
        'jaeger_url': 'http://jaeger:16686'
    }
    
    validator = ObservabilityValidator(config)
    
    try:
        validator.validate_sli_slo()
        validator.validate_log_pipeline()
        validator.validate_alerting_rules()
        print("🎉 All observability validations passed!")
    except Exception as e:
        print(f"💥 Validation failed: {e}")
        exit(1)
```

### 🚀 **3. CI/CD 管道中的測試**

#### **GitLab CI 範例**

```yaml
# .gitlab-ci.yml
stages:
  - build
  - test
  - deploy
  - observability-test

observability-smoke-test:
  stage: observability-test
  image: alpine/curl
  script:
    - apk add --no-cache jq bc
    - ./scripts/observability_health_check.sh
  only:
    - main
  when: on_success

observability-sli-validation:
  stage: observability-test
  image: python:3.9-alpine
  script:
    - pip install requests
    - python scripts/observability_validator.py
  only:
    - main
  when: on_success
  allow_failure: false

chaos-engineering-test:
  stage: observability-test
  image: litmuschaos/litmus-checker:latest
  script:
    - litmus run chaos-experiments/pod-delete.yaml
    - litmus validate --experiment-name pod-delete
  only:
    - schedules
  when: manual
```

#### **GitHub Actions 範例**

```yaml
# .github/workflows/observability-test.yml
name: Observability Testing

on:
  push:
    branches: [main]
  schedule:
    - cron: '0 */6 * * *'  # 每 6 小時執行一次

jobs:
  synthetic-monitoring:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Run Synthetic Tests
        uses: datadog/synthetics-ci-github-action@v1
        with:
          api_key: ${{ secrets.DATADOG_API_KEY }}
          app_key: ${{ secrets.DATADOG_APP_KEY }}
          test_search_query: 'tag:observability'
          
  sli-slo-validation:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Python
        uses: actions/setup-python@v4
        with:
          python-version: '3.9'
          
      - name: Install dependencies
        run: pip install requests prometheus-client
        
      - name: Validate SLI/SLO
        run: python scripts/sli_slo_validator.py
        env:
          PROMETHEUS_URL: ${{ secrets.PROMETHEUS_URL }}
          
  load-test-observability:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Run K6 Load Test
        uses: grafana/k6-action@v0.2.0
        with:
          filename: tests/load-test-observability.js
        env:
          K6_PROMETHEUS_RW_SERVER_URL: ${{ secrets.PROMETHEUS_URL }}
```

### 📊 **4. 基於 K6 的負載測試**

```javascript
// tests/load-test-observability.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// 自定義指標
const errorRate = new Rate('errors');
const observabilityOverhead = new Trend('observability_overhead');

export const options = {
  stages: [
    { duration: '2m', target: 10 },   // 暖身
    { duration: '5m', target: 50 },   // 正常負載
    { duration: '2m', target: 100 },  // 峰值負載
    { duration: '5m', target: 100 },  // 維持峰值
    { duration: '2m', target: 0 },    // 降載
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'], // P95 < 2s
    errors: ['rate<0.01'],             // 錯誤率 < 1%
    observability_overhead: ['avg<50'], // 可觀測性開銷 < 50ms
  },
};

export default function () {
  // 測試主要業務 API
  const startTime = Date.now();
  
  const response = http.get('http://app:8080/api/orders', {
    headers: {
      'X-Correlation-ID': `k6-test-${__VU}-${__ITER}`,
      'X-Load-Test': 'true',
    },
  });
  
  const endTime = Date.now();
  
  // 檢查響應
  const success = check(response, {
    'status is 200': (r) => r.status === 200,
    'response time < 2s': (r) => r.timings.duration < 2000,
    'has correlation header': (r) => r.headers['X-Correlation-ID'] !== undefined,
  });
  
  errorRate.add(!success);
  
  // 測試可觀測性端點的開銷
  const metricsStart = Date.now();
  const metricsResponse = http.get('http://app:8080/actuator/metrics');
  const metricsEnd = Date.now();
  
  observabilityOverhead.add(metricsEnd - metricsStart);
  
  sleep(1);
}

export function handleSummary(data) {
  return {
    'observability-test-results.json': JSON.stringify(data, null, 2),
  };
}
```

### 🔍 **5. 基於 Terraform 的基礎設施測試**

```hcl
# tests/observability_test.tf
resource "test_assertions" "observability_stack" {
  component = "observability"

  equal "cloudwatch_log_groups_exist" {
    description = "CloudWatch log groups should exist"
    got         = length(data.aws_cloudwatch_log_groups.app_logs.log_group_names)
    want        = 3
  }

  equal "prometheus_targets_healthy" {
    description = "All Prometheus targets should be healthy"
    got = length([
      for target in data.prometheus_targets.all.targets :
      target if target.health == "up"
    ])
    want = length(data.prometheus_targets.all.targets)
  }

  check "xray_service_map_exists" {
    description = "X-Ray service map should show our services"
    condition = length(data.aws_xray_service_graph.app.services) > 0
  }
}

# 使用 Terratest 進行測試
resource "null_resource" "run_terratest" {
  provisioner "local-exec" {
    command = "cd tests && go test -v -timeout 30m"
  }
}
```

### 🎯 **業界實際測試策略總結**

| 測試類型 | 主要工具 | 使用場景 | 自動化程度 |
|---------|---------|---------|-----------|
| **Synthetic Monitoring** | DataDog, New Relic, Pingdom | 持續監控用戶體驗 | 🟢 完全自動化 |
| **SLI/SLO 驗證** | Prometheus + Python/Bash | CI/CD 管道品質門檻 | 🟢 完全自動化 |
| **Chaos Engineering** | Chaos Monkey, Litmus | 定期韌性測試 | 🟡 半自動化 |
| **Load Testing** | K6, JMeter, Artillery | 性能和可觀測性開銷測試 | 🟢 完全自動化 |
| **Infrastructure Testing** | Terratest, InSpec | 基礎設施配置驗證 | 🟢 完全自動化 |
| **Manual Validation** | Grafana, Kibana 儀表板 | 深度分析和故障排除 | 🔴 手動操作 |

### 💡 **關鍵洞察**

1. **Java 測試程序主要用於**:
   - 單元測試和集成測試階段
   - 開發環境的快速驗證
   - 特定業務邏輯的可觀測性測試

2. **生產環境主要使用**:
   - 基於腳本的自動化測試 (Bash/Python)
   - 第三方監控工具的 Synthetic Tests
   - CI/CD 管道中的品質門檻檢查
   - Chaos Engineering 工具

3. **最佳實踐組合**:
   - 開發階段: Java 單元測試 + 集成測試
   - CI/CD 階段: 腳本化驗證 + SLI/SLO 檢查
   - 生產階段: Synthetic Monitoring + Chaos Testing
   - 持續改進: 定期手動分析 + 自動化報告

這樣的組合能夠在不同階段提供適當的測試覆蓋，既保證了開發效率，也確保了生產環境的可靠性。
