package solid.humank.genaidemo.testutils.context;

import java.util.HashMap;
import java.util.Map;

/** 場景狀態管理類別，用於管理BDD測試場景的狀態 */
public class ScenarioState {
    private String currentScenario;
    private final Map<String, Object> scenarioData = new HashMap<>();
    private final TestContext testContext;

    public ScenarioState(TestContext testContext) {
        this.testContext = testContext;
    }

    /** 開始新的場景 */
    public void startScenario(String scenarioName) {
        this.currentScenario = scenarioName;
        this.scenarioData.clear();
    }

    /** 結束當前場景 */
    public void endScenario() {
        this.currentScenario = null;
        this.scenarioData.clear();
    }

    /** 取得當前場景名稱 */
    public String getCurrentScenario() {
        return currentScenario;
    }

    /** 設置場景資料 */
    public <T> void setScenarioData(String key, T value) {
        scenarioData.put(key, value);
    }

    /** 取得場景資料 */
    public <T> T getScenarioData(String key, Class<T> type) {
        Object value = scenarioData.get(key);
        if (value == null) {
            return null;
        }
        return type.cast(value);
    }

    /** 檢查是否有場景資料 */
    public boolean hasScenarioData(String key) {
        return scenarioData.containsKey(key);
    }

    /** 取得測試上下文 */
    public TestContext getTestContext() {
        return testContext;
    }

    /** 檢查是否在場景中 */
    public boolean isInScenario() {
        return currentScenario != null;
    }
}
