package solid.humank.genaidemo.bdd.common;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import solid.humank.genaidemo.testutils.bdd.ConsumerShoppingTestHelper;
import solid.humank.genaidemo.testutils.cleanup.TestResourceCleaner;
import solid.humank.genaidemo.testutils.isolation.TestIsolationManager;
import solid.humank.genaidemo.testutils.mocks.MockFactory;
import solid.humank.genaidemo.testutils.stubs.StubFactory;

/**
 * Cucumber 鉤子 - 管理測試場景的生命週期
 * 已改進為支援完整的測試隔離機制
 */
public class CucumberHooks {

    private TestContext testContext;
    private ConsumerShoppingTestHelper shoppingHelper;
    private MockFactory mockFactory;
    private StubFactory stubFactory;
    private TestResourceCleaner resourceCleaner;

    public CucumberHooks() {
        this.testContext = TestContext.getInstance();
        this.shoppingHelper = ConsumerShoppingTestHelper.getInstance();
    }

    @Before
    public void setUp(Scenario scenario) {
        // 初始化測試隔離環境
        String scenarioName = scenario.getName();
        TestIsolationManager.initializeTestContext(scenarioName);

        // 初始化測試工具
        this.mockFactory = MockFactory.getInstance();
        this.stubFactory = StubFactory.getInstance();
        this.resourceCleaner = TestResourceCleaner.getInstance();

        // 清理測試數據
        testContext.clear();
        shoppingHelper.clear();

        // 記錄場景開始
        System.out.println("Starting scenario: " + scenarioName);
    }

    @After
    public void tearDown(Scenario scenario) {
        try {
            // 如果場景失敗，記錄相關資訊
            if (scenario.isFailed()) {
                System.err.println("Scenario failed: " + scenario.getName());

                // 記錄測試上下文狀態
                if (testContext.hasError()) {
                    System.err.println("Test context error: " + testContext.getLastErrorMessage());
                }
            }

            // 清理測試數據
            testContext.clear();
            shoppingHelper.clear();

        } finally {
            // 清理所有測試資源
            if (resourceCleaner != null) {
                resourceCleaner.cleanupAll();
            }

            // 清理測試隔離環境
            TestIsolationManager.cleanupTestContext();

            // 記錄場景結束
            System.out.println("Finished scenario: " + scenario.getName());
        }
    }
}
