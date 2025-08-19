package solid.humank.genaidemo.testutils.bdd;

import solid.humank.genaidemo.bdd.common.TestContext;
import solid.humank.genaidemo.testutils.handlers.TestExceptionHandler;
import solid.humank.genaidemo.testutils.handlers.TestScenarioHandler;
import solid.humank.genaidemo.testutils.mocks.MockFactory;
import solid.humank.genaidemo.testutils.stubs.StubFactory;

/**
 * BDD步驟定義基礎類別，提供共用的測試基礎設施
 * 已改進為支援測試隔離和獨立執行
 */
public abstract class StepDefinitionBase {

    protected final TestContext testContext;
    protected final TestScenarioHandler scenarioHandler;
    protected final TestExceptionHandler exceptionHandler;
    protected final MockFactory mockFactory;
    protected final StubFactory stubFactory;

    protected StepDefinitionBase() {
        this.testContext = TestContext.getInstance();
        this.scenarioHandler = new TestScenarioHandler();
        this.exceptionHandler = new TestExceptionHandler();

        // 初始化測試隔離工具
        this.mockFactory = MockFactory.getInstance();
        this.stubFactory = StubFactory.getInstance();

        // 註冊場景處理器
        registerScenarioProcessors();
    }

    /** 子類別需要實作此方法來註冊特定的場景處理器 */
    protected abstract void registerScenarioProcessors();

    /** 清理測試資料 */
    protected void cleanup() {
        testContext.clear();
        mockFactory.resetAllMocks();
    }

    /** 檢查是否有異常 */
    protected boolean hasException() {
        return testContext.hasError();
    }

    /** 取得最後一個異常訊息 */
    protected String getLastErrorMessage() {
        return testContext.getLastErrorMessage();
    }

    /** 檢查異常訊息是否包含特定文字 */
    protected boolean exceptionMessageContains(String expectedMessage) {
        String lastErrorMessage = getLastErrorMessage();
        return lastErrorMessage != null && lastErrorMessage.contains(expectedMessage);
    }

    /** 安全執行操作 */
    protected void safeExecute(Runnable action) {
        exceptionHandler.handleException(action);
    }

    /** 安全執行操作並返回結果 */
    protected <T> T safeExecute(java.util.function.Supplier<T> action) {
        return exceptionHandler.handleExceptionWithReturn(action);
    }

    /** 設置測試資料到上下文 */
    protected <T> void setTestData(String key, T value) {
        testContext.put(key, value);
    }

    /** 從上下文取得測試資料 */
    protected <T> T getTestData(String key, Class<T> type) {
        return testContext.get(key, type);
    }

    /** 檢查測試資料是否存在 */
    protected boolean hasTestData(String key) {
        return testContext.contains(key);
    }

    /** 創建 Mock 物件 */
    protected <T> T createMock(Class<T> interfaceClass) {
        return mockFactory.createMock(interfaceClass);
    }

    /** 創建 Stub 物件 */
    protected <T> T createStub(Class<T> stubClass) {
        return stubFactory.createStub(stubClass);
    }

}
