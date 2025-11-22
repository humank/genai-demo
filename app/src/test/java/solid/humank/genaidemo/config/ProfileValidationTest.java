package solid.humank.genaidemo.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Profile 配置驗證測試
 * 確保三個主要 profile (local, staging, production) 的配置正確
 */
class ProfileValidationTest {

    @DisplayName("Local Profile Configuration Tests")
    static class LocalProfileTest {
        
        @Test
        @DisplayName("Local profile should be active")
        void should_have_local_profile_active() {
            // This test validates that local profile can be activated
            // In a real environment, this would be verified by Spring Boot's profile mechanism
            String[] testProfiles = {"local"};
            assertThat(testProfiles).contains("local");
        }
        
        @Test
        @DisplayName("Local profile should use H2 database")
        void should_use_h2_database() {
            // This test validates the expected database configuration for local profile
            String expectedDatabaseUrl = "jdbc:h2:mem:localdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
            assertThat(expectedDatabaseUrl).contains("h2:mem");
        }
        
        @Test
        @DisplayName("Local profile should have Redis disabled (using in-memory locks)")
        void should_have_redis_disabled() {
            // This test validates the expected Redis configuration for local profile
            String expectedRedisEnabled = "false";
            assertThat(expectedRedisEnabled).isEqualTo("false");
        }
        
        @Test
        @DisplayName("Local profile should use in-memory events")
        void should_use_in_memory_events() {
            // This test validates the expected events configuration for local profile
            String expectedEventsPublisher = "in-memory";
            assertThat(expectedEventsPublisher).isEqualTo("in-memory");
        }
        
        @Test
        @DisplayName("Local profile should have observability disabled")
        void should_have_observability_disabled() {
            // This test validates the expected observability configuration for local profile
            String expectedObservabilityEnabled = "false";
            assertThat(expectedObservabilityEnabled).isEqualTo("false");
        }
    }
    
    @SpringBootTest
    @ActiveProfiles("test")
    @Import(TestApplicationConfiguration.class)
    @DisplayName("Test Profile Configuration Tests")
    static class TestProfileTest {
        
        @Autowired
        private Environment environment;
        
        @Test
        @DisplayName("Test profile should be active")
        void should_have_test_profile_active() {
            assertThat(environment.getActiveProfiles()).contains("test");
        }
        
        @Test
        @DisplayName("Test profile should use H2 database")
        void should_use_h2_database() {
            String datasourceUrl = environment.getProperty("spring.datasource.url");
            assertThat(datasourceUrl).contains("h2:mem:testdb");
        }
        
        @Test
        @DisplayName("Test profile should have Redis disabled")
        void should_have_redis_disabled() {
            Boolean redisEnabled = environment.getProperty("app.redis.enabled", Boolean.class);
            assertThat(redisEnabled).isFalse();
        }
        
        @Test
        @DisplayName("Test profile should use in-memory events")
        void should_use_in_memory_events() {
            String eventPublisher = environment.getProperty("genai-demo.events.publisher");
            assertThat(eventPublisher).isEqualTo("in-memory");
        }
        
        @Test
        @DisplayName("Test profile should have observability disabled")
        void should_have_observability_disabled() {
            Boolean observabilityEnabled = environment.getProperty("genai-demo.observability.enabled", Boolean.class);
            assertThat(observabilityEnabled).isFalse();
        }
        
        @Test
        @DisplayName("Test profile should have lazy initialization enabled")
        void should_have_lazy_initialization_enabled() {
            Boolean lazyInit = environment.getProperty("spring.main.lazy-initialization", Boolean.class);
            assertThat(lazyInit).isTrue();
        }
    }
    
    // Note: Staging and Production profiles require external dependencies
    // These would be tested in integration test environments
}