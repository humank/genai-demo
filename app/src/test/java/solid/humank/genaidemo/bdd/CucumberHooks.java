package solid.humank.genaidemo.bdd;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;

/**
 * Cucumber 鉤子類，用於在場景執行前後執行一些操作
 * 不依賴 Spring 上下文
 */
public class CucumberHooks {

    @Before
    public void beforeScenario(Scenario scenario) {
        System.out.println("Starting scenario: " + scenario.getName());
        System.out.println("Tags: " + scenario.getSourceTagNames());
    }

    @After
    public void afterScenario(Scenario scenario) {
        System.out.println("Finished scenario: " + scenario.getName());
        System.out.println("Status: " + scenario.getStatus());
    }
}