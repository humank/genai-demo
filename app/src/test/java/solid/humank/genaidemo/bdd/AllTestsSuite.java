package solid.humank.genaidemo.bdd;

import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * 完整測試套件
 * 包含 JUnit 測試和 Cucumber 測試
 */
@Suite
@SuiteDisplayName("All Tests Suite")
@SelectClasses({
    CucumberOrderTestRunner.class,
    CucumberWorkflowTestRunner.class,
    CucumberInventoryTestRunner.class,
    CucumberLogisticsTestRunner.class,
    CucumberNotificationTestRunner.class,
    CucumberPaymentTestRunner.class
})
@IncludeEngines({"junit-jupiter", "cucumber"})
public class AllTestsSuite {
    // 此類不需要任何內容，僅作為測試套件的入口點
}