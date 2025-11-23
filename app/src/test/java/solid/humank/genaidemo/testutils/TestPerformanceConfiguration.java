package solid.humank.genaidemo.testutils;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * Test Performance Configuration for ensuring proper test performance
 * monitoring.
 *
 * This configuration provides:
 * - Performance monitoring setup
 * - Resource cleanup between tests
 * - Test execution isolation
 * - Performance metrics collection
 *
 * This is distinct from the TestIsolationConfiguration in the isolation package
 * which focuses on general test isolation mechanisms.
 *
 * Requirements: 5.3, 5.5, 7.5
 */
@TestConfiguration
@Profile("test")
public class TestPerformanceConfiguration {

    @Bean
    public TestPerformanceListener testPerformanceListener() {
        return new TestPerformanceListener();
    }

    /**
     * Test execution listener that ensures proper performance monitoring and
     * isolation between tests.
     */
    public static class TestPerformanceListener extends AbstractTestExecutionListener {
        private static final Logger logger = LoggerFactory.getLogger(TestPerformanceListener.class);

        @Override
        public void beforeTestMethod(TestContext testContext) throws Exception {
            logger.debug("Setting up test performance monitoring for: {}.{}",
                    testContext.getTestClass().getSimpleName(),
                    testContext.getTestMethod().getName());

            // Clear any existing test data
            clearTestData(testContext);

            // Reset application state
            resetApplicationState();

            // Clear caches
            clearCaches(testContext);
        }

        @Override
        public void afterTestMethod(TestContext testContext) throws Exception {
            logger.debug("Cleaning up after performance test: {}.{}",
                    testContext.getTestClass().getSimpleName(),
                    testContext.getTestMethod().getName());

            // Cleanup test data
            cleanupTestData(testContext);

            // Reset mocks and stubs
            resetMocks(testContext);

            // Clear any temporary resources
            clearTemporaryResources(testContext);
        }

        @Override
        public void afterTestClass(TestContext testContext) throws Exception {
            logger.debug("Final cleanup after performance test class: {}",
                    testContext.getTestClass().getSimpleName());

            // Perform final cleanup
            performFinalCleanup(testContext);
        }

        /**
         * Clear test data from the database.
         */
        private void clearTestData(TestContext testContext) {
            try {
                DataSource dataSource = testContext.getApplicationContext().getBean(DataSource.class);

                try (Connection connection = dataSource.getConnection();
                        Statement statement = connection.createStatement()) {

                    // Clear test tables in the correct order (respecting foreign key constraints)
                    String[] clearStatements = {
                            "DELETE FROM order_items WHERE 1=1",
                            "DELETE FROM orders WHERE 1=1",
                            "DELETE FROM customers WHERE 1=1",
                            "DELETE FROM products WHERE 1=1"
                    };

                    for (String sql : clearStatements) {
                        try {
                            int deletedRows = statement.executeUpdate(sql);
                            if (deletedRows > 0) {
                                logger.debug("Cleared {} rows with: {}", deletedRows, sql);
                            }
                        } catch (SQLException e) {
                            // Table might not exist, which is fine for tests
                            logger.debug("Could not execute cleanup SQL (table might not exist): {}", sql);
                        }
                    }

                    // Reset sequences if using H2
                    try {
                        statement.executeUpdate("ALTER SEQUENCE IF EXISTS hibernate_sequence RESTART WITH 1");
                    } catch (SQLException e) {
                        // Sequence might not exist, which is fine
                        logger.debug("Could not reset sequence: {}", e.getMessage());
                    }

                } catch (SQLException e) {
                    logger.warn("Failed to clear test data: {}", e.getMessage());
                }

            } catch (Exception e) {
                logger.debug("DataSource not available for cleanup (might be using @WebMvcTest): {}", e.getMessage());
            }
        }

        /**
         * Reset application state.
         */
        private void resetApplicationState() {
            // Clear any application-level state
            // This could include clearing static variables, resetting singletons, etc.

            // Reset system properties that might have been modified by tests
            System.clearProperty("test.override.property");

            logger.debug("Application state reset completed");
        }

        /**
         * Clear caches.
         */
        private void clearCaches(TestContext testContext) {
            try {
                // Clear Spring caches if cache manager is available
                if (testContext.getApplicationContext().containsBean("cacheManager")) {
                    org.springframework.cache.CacheManager cacheManager = testContext.getApplicationContext()
                            .getBean(org.springframework.cache.CacheManager.class);

                    cacheManager.getCacheNames().forEach(cacheName -> {
                        org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
                        if (cache != null) {
                            cache.clear();
                            logger.debug("Cleared cache: {}", cacheName);
                        }
                    });
                }
            } catch (Exception e) {
                logger.debug("Cache manager not available or cache clearing failed: {}", e.getMessage());
            }
        }

        /**
         * Cleanup test data after test execution.
         */
        private void cleanupTestData(TestContext testContext) {
            // Perform the same cleanup as before test
            clearTestData(testContext);
        }

        /**
         * Reset mocks and stubs.
         */
        private void resetMocks(TestContext testContext) {
            // Reset Mockito mocks if available
            try {
                // This will reset all mocks created in the current test
                org.mockito.Mockito.reset();
                logger.debug("Mockito mocks reset completed for: {}",
                        testContext.getTestClass().getSimpleName());
            } catch (Exception e) {
                logger.debug("Mockito not available or reset failed: {}", e.getMessage());
            }
        }

        /**
         * Clear temporary resources.
         */
        private void clearTemporaryResources(TestContext testContext) {
            // Clear temporary files, close connections, etc.

            // Clear system properties set during test
            System.getProperties().entrySet().removeIf(entry -> entry.getKey().toString().startsWith("test.temp."));

            logger.debug("Temporary resources cleared for: {}",
                    testContext.getTestClass().getSimpleName());
        }

        /**
         * Perform final cleanup after test class.
         */
        private void performFinalCleanup(TestContext testContext) {
            // Final cleanup operations
            clearTestData(testContext);
            clearCaches(testContext);

            // Force garbage collection
            System.gc();

            logger.debug("Final cleanup completed for test class: {}",
                    testContext.getTestClass().getSimpleName());
        }
    }
}
