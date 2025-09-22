# @TestPerformanceExtension 使用指南

## 概述

`@TestPerformanceExtension` 是專案中的核心測試效能監控工具，提供自動化的測試執行時間追蹤、記憶體使用監控和效能回歸檢測。本指南詳細說明如何使用這個強大的測試效能監控框架。

## 🚀 基本使用

### 標準配置

```java
@TestPerformanceExtension(maxExecutionTimeMs = 10000, maxMemoryIncreaseMB = 100)
@IntegrationTest
public class CustomerServiceIntegrationTest extends BaseIntegrationTest {
    
    @Test
    void should_create_customer_within_performance_limits() {
        // 測試邏輯會自動被監控
        // 執行時間和記憶體使用會被追蹤
        Customer customer = customerService.createCustomer(createCustomerCommand());
        
        assertThat(customer).isNotNull();
        assertThat(customer.getId()).isNotNull();
    }
}
```

### 配置參數說明

| 參數 | 預設值 | 說明 |
|------|--------|------|
| `maxExecutionTimeMs` | 5000ms | 最大允許執行時間 |
| `maxMemoryIncreaseMB` | 50MB | 最大允許記憶體增長 |
| `generateReports` | true | 是否生成詳細報告 |
| `checkRegressions` | true | 是否檢查效能回歸 |

## 📊 監控功能

### 自動監控指標

#### 執行時間追蹤
- **毫秒級精度**: 精確追蹤每個測試方法的執行時間
- **慢測試識別**: 自動標記超過 5 秒的測試
- **超慢測試警告**: 超過 30 秒的測試會被標記為錯誤
- **併發安全**: 支援並行測試執行的時間追蹤

#### 記憶體使用監控
- **堆記憶體追蹤**: 測試前後的堆記憶體使用量對比
- **記憶體增長計算**: 精確計算測試期間的記憶體增長
- **記憶體使用百分比**: 相對於最大可用記憶體的使用百分比
- **自動清理**: 記憶體使用過高時自動觸發清理

### 效能閾值

| 級別 | 執行時間 | 記憶體使用 | 動作 |
|------|----------|------------|------|
| 正常 | < 5s | < 50MB | 正常記錄 |
| 警告 | 5s - 30s | 50MB - 80% 堆記憶體 | 警告日誌 |
| 錯誤 | > 30s | > 80% 堆記憶體 | 錯誤日誌 + 自動清理 |

## 🛠️ 進階配置

### 不同測試類型的配置

#### 單元測試（快速）
```java
@TestPerformanceExtension(maxExecutionTimeMs = 1000, maxMemoryIncreaseMB = 10)
@UnitTest
public class CustomerUnitTest {
    // 快速單元測試的嚴格效能要求
}
```

#### 整合測試（中等）
```java
@TestPerformanceExtension(maxExecutionTimeMs = 10000, maxMemoryIncreaseMB = 100)
@IntegrationTest
public class CustomerIntegrationTest extends BaseIntegrationTest {
    // 整合測試的標準效能要求
}
```

#### E2E 測試（寬鬆）
```java
@TestPerformanceExtension(maxExecutionTimeMs = 30000, maxMemoryIncreaseMB = 200)
@E2ETest
public class CustomerE2ETest extends BaseIntegrationTest {
    // E2E 測試的寬鬆效能要求
}
```

### 自定義配置範例

```java
@TestPerformanceExtension(
    maxExecutionTimeMs = 15000,        // 15 秒超時
    maxMemoryIncreaseMB = 150,         // 150MB 記憶體增長限制
    generateReports = true,            // 生成詳細報告
    checkRegressions = true            // 檢查效能回歸
)
@IntegrationTest
public class ComplexBusinessProcessTest extends BaseIntegrationTest {
    
    @Test
    void should_handle_complex_order_processing() {
        // 複雜的業務流程測試
        // 需要更高的效能閾值
    }
}
```

## 📈 報告系統

### 自動生成的報告

#### 報告位置
```
build/reports/test-performance/
├── performance-report.html              # 互動式 HTML 報告
├── performance-data.csv                 # 原始效能數據
├── overall-performance-summary.txt      # 總體統計摘要
└── {TestClass}-performance-report.txt   # 個別類別報告
```

#### 報告內容

**個別類別報告**：
- 每個測試方法的執行時間
- 記憶體使用詳情
- 失敗原因分析
- 效能趨勢

**總體摘要報告**：
- 執行的測試總數和成功率
- 平均執行時間和記憶體使用
- 前 5 名最慢測試識別
- 效能回歸檢測結果

### 報告生成命令

```bash
# 生成效能報告
./gradlew generatePerformanceReport

# 執行測試並生成報告
./gradlew runAllTestsWithReport

# 查看 HTML 報告
open build/reports/test-performance/performance-report.html

# 查看文字摘要
cat build/reports/test-performance/overall-performance-summary.txt
```

## 🧹 資源管理整合

### 與 BaseIntegrationTest 整合

```java
@TestPerformanceExtension(maxExecutionTimeMs = 10000, maxMemoryIncreaseMB = 100)
@IntegrationTest
public class OrderProcessingTest extends BaseIntegrationTest {
    
    @Test
    void should_process_order_efficiently() {
        // 測試邏輯
        Order order = orderService.processOrder(createOrderCommand());
        
        // 自動效能監控
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PROCESSED);
    }
    
    @AfterEach
    void cleanup() {
        // 檢查記憶體使用並清理
        if (!isMemoryUsageAcceptable()) {
            forceResourceCleanup();
        }
    }
}
```

### 資源管理方法

```java
// BaseIntegrationTest 提供的資源管理方法
protected void forceResourceCleanup() {
    // 強制清理測試資源
}

protected boolean isMemoryUsageAcceptable() {
    // 檢查記憶體使用是否可接受
}

protected void waitForCondition(BooleanSupplier condition, Duration timeout, String description) {
    // 等待異步操作完成
}
```

## 🔍 故障排除

### 常見問題和解決方案

#### 1. 測試執行超時

**問題**: 測試執行時間超過設定的 `maxExecutionTimeMs`

**解決方案**:
```java
// 增加超時時間
@TestPerformanceExtension(maxExecutionTimeMs = 20000) // 增加到 20 秒

// 或者優化測試邏輯
@Test
void should_process_order_quickly() {
    // 使用 Mock 減少外部依賴
    when(externalService.call()).thenReturn(mockResponse());
    
    // 測試邏輯
}
```

#### 2. 記憶體使用超標

**問題**: 測試記憶體增長超過設定的 `maxMemoryIncreaseMB`

**解決方案**:
```java
@Test
void should_handle_large_dataset() {
    // 在測試中主動清理
    try {
        // 測試邏輯
        processLargeDataset();
    } finally {
        // 主動清理資源
        forceResourceCleanup();
    }
}
```

#### 3. 效能回歸檢測

**問題**: 效能回歸檢測報告測試變慢

**解決方案**:
1. 查看效能報告識別瓶頸
2. 優化資料庫查詢
3. 減少不必要的 Spring 上下文載入
4. 使用更多的 Mock 減少外部依賴

### 監控和調試

#### 啟用詳細日誌

```java
// 在測試中啟用詳細的效能日誌
@TestPerformanceExtension(
    maxExecutionTimeMs = 10000,
    maxMemoryIncreaseMB = 100,
    generateReports = true,
    checkRegressions = true
)
@Slf4j
public class DebuggingTest extends BaseIntegrationTest {
    
    @Test
    void should_debug_performance_issue() {
        log.info("Starting performance-critical test");
        
        // 測試邏輯
        
        log.info("Test completed, check performance report");
    }
}
```

#### 效能分析

```bash
# 查看詳細的效能分析
./gradlew test --info

# 生成並查看效能報告
./gradlew generatePerformanceReport
open build/reports/test-performance/performance-report.html
```

## 🎯 最佳實踐

### 1. 選擇適當的閾值

```java
// 根據測試類型選擇合適的閾值
@TestPerformanceExtension(maxExecutionTimeMs = 1000, maxMemoryIncreaseMB = 10)  // 單元測試
@TestPerformanceExtension(maxExecutionTimeMs = 5000, maxMemoryIncreaseMB = 50)  // 整合測試
@TestPerformanceExtension(maxExecutionTimeMs = 15000, maxMemoryIncreaseMB = 150) // E2E 測試
```

### 2. 結合資源管理

```java
@TestPerformanceExtension(maxExecutionTimeMs = 10000, maxMemoryIncreaseMB = 100)
public class OptimizedTest extends BaseIntegrationTest {
    
    @AfterEach
    void ensureResourceCleanup() {
        if (!isMemoryUsageAcceptable()) {
            forceResourceCleanup();
        }
    }
}
```

### 3. 定期檢查效能報告

```bash
# 定期生成和檢查效能報告
./gradlew generatePerformanceReport

# 在 CI/CD 中整合效能檢查
./gradlew test generatePerformanceReport
```

### 4. 效能回歸預防

- 在 CI/CD 管道中啟用效能監控
- 定期檢查效能趨勢報告
- 設置效能回歸警報
- 建立效能基準線

## 🔗 相關資源

### 內部文檔
- [測試策略總覽](../README.md) - 整體測試策略
- [整合測試指南](../integration-testing.md) - 整合測試最佳實踐
- [BaseIntegrationTest 使用指南](../base-integration-test.md) - 基礎測試類別

### 效能標準
- [效能標準](../../../../.kiro/steering/performance-standards.md) - 整體效能要求
- [測試效能標準](../../../../.kiro/steering/test-performance-standards.md) - 測試效能詳細標準

---

**最後更新**: 2025年1月21日  
**維護者**: QA Team  
**版本**: 1.0

> 💡 **提示**: 效能監控不僅是為了發現問題，更是為了建立效能意識和持續改進的文化。讓每個測試都成為效能的守護者。