package solid.humank.genaidemo.bdd;

import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * JUnit Platform Suite 測試套件
 * 用於運行所有 BDD 相關的測試
 */
@Suite
@SuiteDisplayName("Feature File Validation Test Suite")
@SelectPackages("solid.humank.genaidemo.bdd.steps")
public class FeatureTestSuite {
    // 此類不需要任何內容，僅作為測試套件的入口點
}