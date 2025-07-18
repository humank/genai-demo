package solid.humank.genaidemo.testutils.handlers;

/**
 * 場景處理器介面，定義場景處理的基本契約
 */
@FunctionalInterface
public interface ScenarioProcessor {
    
    /**
     * 處理場景
     * 
     * @param params 場景參數
     * @return 處理結果
     */
    <T> T process(Object... params);
}