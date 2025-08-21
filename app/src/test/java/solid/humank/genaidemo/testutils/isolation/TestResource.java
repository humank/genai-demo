package solid.humank.genaidemo.testutils.isolation;

/**
 * 測試資源介面
 * 所有需要在測試結束後清理的資源都應該實現此介面
 */
public interface TestResource {

    /**
     * 清理資源
     * 此方法會在測試結束後自動調用
     */
    void cleanup() throws Exception;

    /**
     * 獲取資源名稱
     */
    default String getResourceName() {
        return this.getClass().getSimpleName();
    }

    /**
     * 檢查資源是否已經被清理
     */
    default boolean isCleanedUp() {
        return false;
    }
}