package solid.humank.genaidemo.bdd;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/**
 * 使用 JUnit 5 模式運行 Cucumber 測試
 * 專門用於運行 notification 相關的 feature 文件
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/notification")
@IncludeTags("notification")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "solid.humank.genaidemo.bdd.steps.notification")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty, html:build/reports/cucumber/notification-report.html, json:build/reports/cucumber/notification-report.json")
public class CucumberNotificationTestRunner {
    // 此類作為 Notification 相關 Cucumber 測試的入口點
    // 不需要任何方法，僅用於配置
}