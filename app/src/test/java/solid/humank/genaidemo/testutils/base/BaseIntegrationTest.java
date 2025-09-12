package solid.humank.genaidemo.testutils.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for integration tests
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected void logTestStart(String testName) {
        logger.info("Starting test: {}", testName);
    }

    protected void logTestEnd(String testName) {
        logger.info("Completed test: {}", testName);
    }
}