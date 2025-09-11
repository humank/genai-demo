package solid.humank.genaidemo.bdd;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import io.cucumber.spring.CucumberContextConfiguration;

/**
 * Spring configuration for Cucumber tests
 * This class enables Spring Boot integration for all Cucumber step definitions
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class CucumberSpringConfiguration {
    // This class is used to configure Spring context for Cucumber tests
    // No implementation needed - just annotations
}