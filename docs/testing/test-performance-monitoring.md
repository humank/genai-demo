# 測試性能監控框架

## 概述

本專案實現了完整的測試性能監控框架，自動追蹤測試執行時間、記憶體使用量，並生成詳細的性能分析報告。

## 🚀 核心功能

### TestPerformanceExtension

自動化測試性能監控的核心組件：

```java
@TestPerformanceExtension(maxExecutionTimeMs = 10000, maxMemoryIncreaseMB = 100)
@IntegrationTest
public class MyIntegrationTest extends BaseIntegrationTest {
    // 測試方法會自動被監控性能
    
    @Test
    void should_process_order_within_performance_limits() {
        // 測試邏輯
        // 自動監控執行時間和記憶體使用
    }
}
```

### 配置選項

- `maxExecutionTimeMs`: 最大允許執行時間（預設：5000ms）
- `maxMemoryIncreaseMB`: 最大允許記憶體增長（預設：50MB）
- `generateReports`: 是否生成詳細報告（預設：true）
- `checkRegressions`: 是否檢查性能回歸（預設：true）

## 📊 監控指標

### 執行時間追蹤

- **毫秒級精度**: 精確追蹤每個測試方法的執行時間
- **慢測試識別**: 自動標記超過 5 秒的測試為慢測試
- **超慢測試警告**: 超過 30 秒的測試會被標記為錯誤

### 記憶體使用監控

- **堆記憶體追蹤**: 測試前後的堆記憶體使用量對比
- **記憶體增長計算**: 精確計算測試期間的記憶體增長
- **記憶體使用百分比**: 相對於最大可用記憶體的使用百分比

### 性能閾值

- **慢測試警告**: > 5 秒
- **超慢測試錯誤**: > 30 秒
- **記憶體使用警告**: > 50MB 增長
- **記憶體使用嚴重**: > 80% 可用堆記憶體

## 🛠️ 核心組件

### TestPerformanceMonitor

JUnit 5 擴展，提供全面的測試性能監控：

```java
public class TestPerformanceMonitor implements BeforeAllCallback, AfterAllCallback,
        BeforeEachCallback, AfterEachCallback, TestWatcher {
    
    // 自動追蹤：
    // - 測試執行時間
    // - 測試期間記憶體使用
    // - 性能回歸
    // - 資源清理
}
```

**功能特色**：

- 併發測試執行追蹤（線程安全的數據結構）
- 自動報告生成在 `build/reports/test-performance/`
- 性能回歸檢測
- 慢測試識別和警告

### TestPerformanceResourceManager

測試資源監控和管理組件：

```java
@TestComponent
public class TestPerformanceResourceManager {
    
    public ResourceUsageStats getResourceUsageStats() {
        // 返回當前資源使用統計：
        // - 當前記憶體使用和最大可用記憶體
        // - 記憶體使用百分比
        // - 活躍測試資源數量
    }
    
    public void forceCleanup() {
        // 強制清理所有測試資源
        // 觸發 System.gc() 釋放記憶體
    }
}
```

### TestPerformanceConfiguration

Spring 測試配置，用於性能監控設置：

```java
@TestConfiguration
@Profile("test")
public class TestPerformanceConfiguration {
    
    @Bean
    public TestPerformanceListener testPerformanceListener() {
        return new TestPerformanceListener();
    }
}
```

**TestPerformanceListener 提供**：

- 每個測試方法前後的自動清理
- 資料庫清理（正確處理外鍵約束）
- 快取清理
- Mock 重置功能
- 應用程式狀態重置
- 臨時資源清理
- 測試類完成後的最終清理

## 📈 報告系統

### 報告結構

```
build/reports/test-performance/
├── performance-report.html          # 互動式 HTML 報告（透過 TestPerformanceReportGenerator）
├── performance-data.csv             # 原始性能數據（透過 TestPerformanceReportGenerator）
├── overall-performance-summary.txt  # 總體統計（透過 TestPerformanceMonitor）
└── {TestClass}-performance-report.txt # 個別類別報告（透過 TestPerformanceMonitor）
```

### 報告內容

#### 個別類別報告

- 測試執行時間
- 記憶體使用情況
- 失敗原因分析

#### 總體摘要報告

- 執行的測試總數
- 成功率統計
- 平均執行時間
- 性能分析：慢測試識別、前 5 名最慢測試

#### HTML 報告

- 互動式圖表和詳細分析（單獨生成）
- 視覺化性能趨勢
- 可鑽取的詳細數據

#### CSV 數據

- 原始性能數據，供進一步分析使用
- 可匯入其他分析工具

## 🔧 Gradle 測試任務配置

### 優化的測試任務

```gradle
// 單元測試 - 日常開發的快速回饋
tasks.register('unitTest', Test) {
    description = '快速單元測試 (~5MB, ~50ms 每個)'
    useJUnitPlatform {
        excludeTags 'integration', 'end-to-end', 'slow'
        includeTags 'unit'
    }
    maxHeapSize = '2g'
    maxParallelForks = Runtime.runtime.availableProcessors()
    forkEvery = 0  // 不重啟 JVM 以提高速度
}

// 整合測試 - 提交前驗證
tasks.register('integrationTest', Test) {
    description = '整合測試 (~50MB, ~500ms 每個)'
    useJUnitPlatform {
        includeTags 'integration'
        excludeTags 'end-to-end', 'slow'
    }
    maxHeapSize = '6g'
    minHeapSize = '2g'
    maxParallelForks = 1
    forkEvery = 5
    timeout = Duration.ofMinutes(30)
    
    // HttpComponents 優化和 JVM 調優
    jvmArgs += [
        '--enable-preview',
        '-XX:MaxMetaspaceSize=1g',
        '-XX:+UseG1GC',
        '-XX:+UseStringDeduplication',
        '-XX:G1HeapRegionSize=32m',
        // HttpComponents 特定 JVM 參數
        '-Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.SimpleLog',
        '-Dsun.net.useExclusiveBind=false',
        '-Djava.net.preferIPv4Stack=true'
    ]
    
    // 整合測試的增強系統屬性
    systemProperties = [
        'junit.jupiter.execution.timeout.default': '2m',
        'spring.profiles.active': 'test',
        'test.resource.cleanup.enabled': 'true',
        'test.memory.monitoring.enabled': 'true'
    ]
}
```

### 測試任務階層

```bash
# 開發工作流程
./gradlew quickTest              # 日常開發（< 2 分鐘）
./gradlew preCommitTest          # 提交前驗證（< 5 分鐘）
./gradlew fullTest               # 發布前驗證（< 30 分鐘）

# 特定測試類型
./gradlew unitTest               # 僅單元測試
./gradlew integrationTest        # 僅整合測試
./gradlew e2eTest               # 僅端到端測試
./gradlew cucumber              # BDD Cucumber 測試
```

## 🎯 性能閾值和監控

### 性能閾值

- **慢測試警告**: > 5 秒
- **超慢測試錯誤**: > 30 秒
- **記憶體使用警告**: > 50MB 增長
- **記憶體使用嚴重**: > 80% 可用堆記憶體

### 自動性能監控

#### 測試執行監控

```java
// TestPerformanceMonitor 自動監控
public class TestPerformanceMonitor implements BeforeAllCallback, AfterAllCallback,
        BeforeEachCallback, AfterEachCallback, TestWatcher {
    
    // 自動追蹤：
    // - 測試執行時間
    // - 測試期間記憶體使用
    // - 性能回歸
    // - 資源清理
}
```

#### 性能報告生成

- **HTML 報告**: 互動式圖表和詳細分析
- **CSV 匯出**: 原始數據供進一步分析
- **趨勢分析**: 性能回歸檢測
- **資源使用**: 記憶體和 CPU 使用率追蹤

## 🧹 測試資源管理

### 資源清理策略

#### 自動清理

```java
// TestPerformanceConfiguration 提供自動清理
public static class TestPerformanceListener extends AbstractTestExecutionListener {
    
    @Override
    public void afterTestMethod(TestContext testContext) throws Exception {
        // 每個測試方法後的自動清理：
        // - 資料庫清理
        // - 快取清理
        // - Mock 重置
        // - 臨時資源清理
    }
}
```

#### 手動資源管理

```java
// BaseIntegrationTest 提供手動資源管理
protected void forceResourceCleanup() {
    // 測試期間需要時強制清理
}

protected boolean isMemoryUsageAcceptable() {
    // 檢查記憶體使用是否在可接受範圍內
}

protected void waitForCondition(BooleanSupplier condition, Duration timeout, String description) {
    // 等待異步操作完成，設定超時
}
```

### 記憶體管理最佳實踐

#### 測試的 JVM 配置

```gradle
// 測試執行的優化 JVM 參數
jvmArgs += [
    '--enable-preview',
    '-XX:MaxMetaspaceSize=1g',
    '-XX:+UseG1GC',
    '-XX:+UseStringDeduplication',
    '-XX:G1HeapRegionSize=32m',
    '-XX:+UnlockExperimentalVMOptions',
    '-XX:G1NewSizePercent=20',
    '-XX:G1MaxNewSizePercent=30',
    '-Xshare:off'
]
```

#### 記憶體監控

- **警告閾值**: 80% 記憶體使用
- **嚴重閾值**: 90% 記憶體使用
- **自動 GC**: 在嚴重使用時觸發
- **定期清理**: 每 5 個測試清理一次

## 📊 使用範例

### 基本使用

```java
@TestPerformanceExtension(maxExecutionTimeMs = 5000, maxMemoryIncreaseMB = 50)
@IntegrationTest
public class OrderProcessingTest extends BaseIntegrationTest {
    
    @Test
    void should_process_order_quickly() {
        // 測試邏輯
        // 自動監控性能
    }
}
```

### 進階配置

```java
@TestPerformanceExtension(
    maxExecutionTimeMs = 10000,
    maxMemoryIncreaseMB = 100,
    generateReports = true,
    checkRegressions = true
)
@IntegrationTest
public class ComplexIntegrationTest extends BaseIntegrationTest {
    
    @Test
    void should_handle_complex_scenario() {
        // 複雜測試邏輯
        // 更高的性能閾值
    }
}
```

### 性能報告生成

```bash
# 生成性能報告
./gradlew generatePerformanceReport

# 執行所有測試並生成報告
./gradlew runAllTestsWithReport

# 查看報告
open build/reports/test-performance/performance-report.html
```

## 🔍 故障排除

### 常見問題

#### 記憶體不足

- 增加測試 JVM 堆記憶體大小
- 檢查測試中的記憶體洩漏
- 使用 `forceResourceCleanup()` 手動清理

#### 測試執行緩慢

- 檢查慢測試報告
- 優化資料庫查詢
- 減少不必要的 Spring 上下文載入

#### 性能回歸

- 查看性能趨勢報告
- 比較歷史執行數據
- 識別性能瓶頸

### 監控和報告

#### 性能指標

- **測試執行時間**: 每個測試和每個類別
- **記憶體使用**: 每個測試前後
- **資源利用率**: CPU、記憶體、資料庫連接
- **失敗率**: 成功/失敗統計

#### 報告生成

```bash
# 生成性能報告
./gradlew generatePerformanceReport

# 查看報告
open build/reports/test-performance/performance-report.html
```

#### 性能回歸檢測

- 自動檢測超過閾值的測試
- 歷史性能比較
- 趨勢分析和警報
- 與 CI/CD 管道整合

這個框架確保整個應用程式的測試性能一致、受監控且優化。
