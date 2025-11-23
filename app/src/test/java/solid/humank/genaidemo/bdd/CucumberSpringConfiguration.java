package solid.humank.genaidemo.bdd;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import solid.humank.genaidemo.GenAiDemoApplication;

/**
 * Cucumber Spring Configuration
 * Provides Spring context for Cucumber BDD tests
 */
@CucumberContextConfiguration
@SpringBootTest(
    classes = GenAiDemoApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
public class CucumberSpringConfiguration {
    // No additional configuration needed - Spring Boot auto-configuration handles everything
}
