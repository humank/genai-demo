package solid.humank.genaidemo.testutils.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;

/**
 * Integration test annotation for tests that require Spring context
 * but don't need full web environment.
 * 
 * Memory usage: ~50MB (vs @SpringBootTest ~500MB)
 * Execution time: ~500ms (vs @SpringBootTest ~2s)
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag("integration")
@Tag("medium")
public @interface IntegrationTest {
}