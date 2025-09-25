package solid.humank.genaidemo.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Profile 配置驗證測試
 * 確保三個主要 profile (local, staging, production) 的配置正確
 */
class ProfileValidationTest {

    @SpringBootTest
    @ActiveProfiles("local")
    @DisplayName("Local Profile Configuration Tests")
    static class LocalProfileTest {
        
        @Autowired
        private Environment environment;
        
        @Test
        @DisplayName("Local profile should be active")
        void should_have_local_profile_active() {
            assertThat(environment.getActiveProfiles()).contains("local");
        }
        
        @Test
        @DisplayName("Local profile should use H2 database")
        void should_use_h2_database() {
            String datasourceUrl = environment.getProperty("spring.datasource.url");
            assertThat(datasourceUrl).contains("h2:mem");
        }
        
        @Test
        @DisplayName("Local profile should have Redis disabled (using in-memory locks)")
        void should_have_redis_disabled() {
            Boolean redisEnabled = environment.getProperty("app.redis.enabled", Boolean.class);
            assertThat(redisEnabled).isFalse();
        }
        
        @Test
        @DisplayName("Local profile should use in-memory events")
        void should_use_in_memory_events() {
            String eventPublisher = environment.getProperty("genai-demo.events.publisher");
            assertThat(eventPublisher).isEqualTo("in-memory");
        }
        
        @Test
        @DisplayName("Local profile should have observability disabled")
        void should_have_observability_disabled() {
            Boolean observabilityEnabled = environment.getProperty("genai-demo.observability.enabled", Boolean.class);
            assertThat(observabilityEnabled).isFalse();
        }
    }
    
    @SpringBootTest
    @ActiveProfiles("test")
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