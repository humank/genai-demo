package solid.humank.genaidemo.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * 最簡化的健康檢查測試 - 只測試 Spring 上下文載入
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.main.lazy-initialization=true",
        "logging.level.root=ERROR",
        "genai-demo.observability.enabled=false",
        "genai-demo.events.publisher=in-memory",
        "genai-demo.events.async=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration"
})
public class MinimalHealthTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void shouldLoadApplicationContext() {
        // 測試 Spring 應用程式上下文是否能正常載入
        assertThat(applicationContext).isNotNull();
        assertThat(applicationContext.getBeanDefinitionCount()).isGreaterThan(0);
    }
}