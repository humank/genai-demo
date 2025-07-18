package solid.humank.genaidemo.testutils.bdd;

import solid.humank.genaidemo.testutils.context.ScenarioState;
import solid.humank.genaidemo.testutils.context.TestContext;
import solid.humank.genaidemo.testutils.handlers.TestExceptionHandler;
import solid.humank.genaidemo.testutils.handlers.TestScenarioHandler;

/**
 * BDD步驟定義基礎類別，提供共用的測試基礎設施
 */
public abstract class StepDefinitionBase {
    
    protected final TestContext testContext;
    protected final ScenarioState scenarioState;
    protected final TestScenarioHandler scenarioHandler;
    protected final TestExceptionHandler exceptionHandler;
    
    protected StepDefinitionBase() {
        this.testContext = new TestContext();
        this.scenarioState = new ScenarioState(testContext);
        this.scenarioHandler = new TestScenarioHandler(testContext);
        this.exceptionHandler = new TestExceptionHandler(testContext);
        
        // 註冊場景處理器
        registerScenarioProcessors();
    }
    
    /**
     * 子類別需要實作此方法來註冊特定的場景處理器
     */
    protected abstract void registerScenarioProcessors();
    
    /**
     * 清理測試資料
     */
    protected void cleanup() {
        testContext.clear();
        scenarioState.endScenario();
    }
    
    /**
     * 檢查是否有異常
     */
    protected boolean hasException() {
        return testContext.hasException();
    }
    
    /**
     * 取得最後一個異常
     */
    protected Exception getLastException() {
        return testContext.getLastException();
    }
    
    /**
     * 檢查異常訊息是否包含特定文字
     */
    protected boolean exceptionMessageContains(String expectedMessage) {
        Exception lastException = getLastException();
        return lastException != null && 
               lastException.getMessage() != null && 
               lastException.getMessage().contains(expectedMessage);
    }
    
    /**
     * 安全執行操作
     */
    protected void safeExecute(Runnable action) {
        exceptionHandler.handleException(action);
    }
    
    /**
     * 安全執行操作並返回結果
     */
    protected <T> T safeExecute(java.util.function.Supplier<T> action) {
        return exceptionHandler.handleExceptionWithReturn(action);
    }
    
    /**
     * 設置測試資料到上下文
     */
    protected <T> void setTestData(String key, T value) {
        testContext.put(key, value);
    }
    
    /**
     * 從上下文取得測試資料
     */
    protected <T> T getTestData(String key, Class<T> type) {
        return testContext.get(key, type);
    }
    
    /**
     * 檢查測試資料是否存在
     */
    protected boolean hasTestData(String key) {
        return testContext.contains(key);
    }
}