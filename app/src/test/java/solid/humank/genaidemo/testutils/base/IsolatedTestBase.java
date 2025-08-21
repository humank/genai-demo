package solid.humank.genaidemo.testutils.base;

import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import solid.humank.genaidemo.testutils.cleanup.TestResourceCleaner;
import solid.humank.genaidemo.testutils.database.TestDatabaseManager;
import solid.humank.genaidemo.testutils.isolation.TestExecutionMonitor;
import solid.humank.genaidemo.testutils.isolation.TestIsolationConfiguration;
import solid.humank.genaidemo.testutils.isolation.TestIsolationManager;
import solid.humank.genaidemo.testutils.mocks.MockFactory;
import solid.humank.genaidemo.testutils.stubs.StubFactory;

/**
 * 隔離測試基礎類別
 * 提供完整的測試隔離機制，確保每個測試都有獨立的執行環境
 * 
 * 改進功能：
 * 1. 測試獨立執行機制
 * 2. 測試間隔離機制
 * 3. 事件在測試範圍內的正確隔離
 * 4. Mock 和 Stub 的正確使用模式
 * 5. 測試資源的自動清理機制
 */
public abstract class IsolatedTestBase {

    protected MockFactory mockFactory;
    protected StubFactory stubFactory;
    protected TestResourceCleaner resourceCleaner;
    protected TestDatabaseManager databaseManager;
    protected TestIsolationConfiguration configuration;

    private LocalDateTime testStartTime;
    private String currentTestId;

    @BeforeEach
    void setUpIsolation(TestInfo testInfo) {
        testStartTime = LocalDateTime.now();

        // 初始化測試隔離環境
        String testName = testInfo.getDisplayName();
        TestIsolationManager.initializeTestContext(testName);

        // 獲取當前測試上下文
        TestIsolationManager.TestExecutionContext context = TestIsolationManager.getCurrentContext();
        currentTestId = context.getTestId();

        // 記錄測試開始
        TestExecutionMonitor.getInstance().recordTestStart(currentTestId, testName);

        // 初始化配置
        this.configuration = getTestConfiguration();

        // 初始化測試工具
        this.mockFactory = MockFactory.getInstance();
        this.stubFactory = StubFactory.getInstance();
        this.resourceCleaner = TestResourceCleaner.getInstance();
        this.databaseManager = TestDatabaseManager.getInstance();

        // 註冊資源到清理器
        resourceCleaner.registerResource(mockFactory);
        resourceCleaner.registerResource(stubFactory);
        resourceCleaner.registerResource(databaseManager);

        // 初始化資料庫
        databaseManager.initializeDatabase();

        // 執行子類別的初始化
        try {
            setupTest();
        } catch (Exception e) {
            // 如果初始化失敗，記錄並重新拋出
            recordTestFailure(e.getMessage());
            throw new RuntimeException("Test setup failed", e);
        }
    }

    @AfterEach
    void tearDownIsolation() {
        LocalDateTime cleanupStartTime = LocalDateTime.now();

        try {
            // 執行子類別的清理
            cleanupTest();
        } catch (Exception e) {
            System.err.println("Warning: Test cleanup failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                // 清理所有測試資源
                resourceCleaner.cleanupAll();

                // 清理測試隔離環境
                TestIsolationManager.cleanupTestContext();

                // 記錄測試成功（如果沒有異常）
                LocalDateTime endTime = LocalDateTime.now();
                Duration executionTime = Duration.between(testStartTime, cleanupStartTime);
                Duration cleanupTime = Duration.between(cleanupStartTime, endTime);

                TestExecutionMonitor.getInstance().recordTestSuccess(currentTestId, executionTime, cleanupTime);

            } catch (Exception e) {
                recordTestFailure("Cleanup failed: " + e.getMessage());
                System.err.println("Error during test isolation cleanup: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * 子類別可以覆寫此方法來執行測試前的初始化
     */
    protected void setupTest() {
        // 預設為空實作
    }

    /**
     * 子類別可以覆寫此方法來執行測試後的清理
     */
    protected void cleanupTest() {
        // 預設為空實作
    }

    /**
     * 子類別可以覆寫此方法來提供自定義配置
     */
    protected TestIsolationConfiguration getTestConfiguration() {
        return TestIsolationConfiguration.getDefault();
    }

    /**
     * 創建 Mock 物件
     */
    protected <T> T createMock(Class<T> interfaceClass) {
        return mockFactory.createMock(interfaceClass);
    }

    /**
     * 創建 Stub 物件
     */
    protected <T> T createStub(Class<T> stubClass) {
        return stubFactory.createStub(stubClass);
    }

    /**
     * 註冊清理任務
     */
    protected void registerCleanupTask(Runnable task) {
        resourceCleaner.registerCleanupTask(task);
    }

    /**
     * 獲取當前測試上下文
     */
    protected TestIsolationManager.TestExecutionContext getTestContext() {
        return TestIsolationManager.getCurrentContext();
    }

    /**
     * 設定測試屬性
     */
    protected void setTestAttribute(String key, Object value) {
        getTestContext().setAttribute(key, value);
    }

    /**
     * 獲取測試屬性
     */
    protected <T> T getTestAttribute(String key, Class<T> type) {
        return getTestContext().getAttribute(key, type);
    }

    /**
     * 檢查測試屬性是否存在
     */
    protected boolean hasTestAttribute(String key) {
        return getTestContext().hasAttribute(key);
    }

    /**
     * 驗證測試隔離狀態
     */
    protected void verifyTestIsolation() {
        // 驗證資料庫隔離
        if (!databaseManager.isEmpty()) {
            System.out.println("Warning: Test database is not empty");
        }

        // 驗證 Mock 狀態
        // Mock 工廠會自動重置，無需額外驗證
    }

    /**
     * 強制清理測試環境
     */
    protected void forceCleanup() {
        if (databaseManager != null) {
            databaseManager.cleanupDatabase();
        }
        if (mockFactory != null) {
            mockFactory.resetAllMocks();
        }
    }

    /**
     * 記錄測試失敗
     */
    private void recordTestFailure(String errorMessage) {
        if (currentTestId != null && testStartTime != null) {
            Duration executionTime = Duration.between(testStartTime, LocalDateTime.now());
            TestExecutionMonitor.getInstance().recordTestFailure(currentTestId, executionTime, errorMessage);
        }
    }

    /**
     * 獲取測試執行統計
     */
    protected TestExecutionMonitor.TestExecutionStats getExecutionStats() {
        return TestExecutionMonitor.getInstance().getExecutionStats();
    }

    /**
     * 安全執行操作，捕獲異常但不中斷測試
     */
    protected void safeExecute(Runnable operation) {
        try {
            operation.run();
        } catch (Exception e) {
            System.err.println("Warning: Safe execution failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 安全執行操作並返回結果
     */
    protected <T> T safeExecute(java.util.function.Supplier<T> operation, T defaultValue) {
        try {
            return operation.get();
        } catch (Exception e) {
            System.err.println("Warning: Safe execution failed: " + e.getMessage());
            e.printStackTrace();
            return defaultValue;
        }
    }
}