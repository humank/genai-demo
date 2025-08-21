package solid.humank.genaidemo.bdd.hooks;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import solid.humank.genaidemo.bdd.common.TestContext;
import solid.humank.genaidemo.testutils.database.TestDatabaseManager;
import solid.humank.genaidemo.testutils.mocks.MockFactory;

/**
 * 測試 Hooks - 管理測試生命週期
 * 確保每個 BDD 場景都有乾淨的測試環境，避免測試間相互影響
 */
public class TestHooks {

    private TestContext testContext = TestContext.getInstance();
    private TestDatabaseManager databaseManager = TestDatabaseManager.getInstance();
    private MockFactory mockFactory = MockFactory.getInstance();

    @Before
    public void setUp() {
        // 每個場景開始前進行完整的環境初始化

        // 1. 清理測試上下文
        testContext.clear();

        // 2. 初始化測試資料庫
        databaseManager.initializeDatabase();

        // 3. 重置所有 Mock 物件
        mockFactory.resetAllMocks();

        // 4. 開始新的測試事務
        databaseManager.beginTransaction();
    }

    @After
    public void tearDown() {
        try {
            // 每個場景結束後進行完整的環境清理

            // 1. 回滾測試事務（如果有的話）
            databaseManager.rollbackTransaction();

            // 2. 清理測試資料庫
            databaseManager.cleanupDatabase();

            // 3. 重置所有 Mock 物件
            mockFactory.resetAllMocks();

            // 4. 清理測試上下文
            testContext.clear();

        } catch (Exception e) {
            // 確保即使清理過程中出現異常，也不會影響其他測試
            System.err.println("Warning: Error during test cleanup: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
