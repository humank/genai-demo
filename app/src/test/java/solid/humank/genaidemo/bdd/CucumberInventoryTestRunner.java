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
 * 專門用於運行 inventory 相關的 feature 文件
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/inventory")
@IncludeTags("inventory")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "solid.humank.genaidemo.bdd.steps.inventory")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty, html:build/reports/cucumber/inventory-report.html, json:build/reports/cucumber/inventory-report.json")
public class CucumberInventoryTestRunner {
    // 此類作為 Inventory 相關 Cucumber 測試的入口點
    // 不需要任何方法，僅用於配置
}