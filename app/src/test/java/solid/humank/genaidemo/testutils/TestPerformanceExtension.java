package solid.humank.genaidemo.testutils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Annotation to enable test performance monitoring and reporting.
 * 
 * Usage:
 * 
 * <pre>
 * {@code
 * @TestPerformanceExtension
 * class MyIntegrationTest {
 *     // Test methods will be automatically monitored
 * }
 * }
 * </pre>
 * 
 * This extension provides:
 * - Automatic test execution time monitoring
 * - Memory usage tracking
 * - Performance regression detection
 * - Detailed execution reports
 * 
 * Requirements: 5.1, 5.4
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(TestPerformanceMonitor.class)
public @interface TestPerformanceExtension {

    /**
     * Maximum allowed execution time in milliseconds.
     * If a test exceeds this time, a warning will be logged.
     * Default is 5000ms (5 seconds).
     */
    long maxExecutionTimeMs() default 5000;

    /**
     * Maximum allowed memory increase in MB.
     * If a test uses more memory than this, a warning will be logged.
     * Default is 50MB.
     */
    long maxMemoryIncreaseMB() default 50;

    /**
     * Whether to generate detailed reports for this test.
     * Default is true.
     */
    boolean generateReports() default true;

    /**
     * Whether to check for performance regressions.
     * Default is true.
     */
    boolean checkRegressions() default true;
}