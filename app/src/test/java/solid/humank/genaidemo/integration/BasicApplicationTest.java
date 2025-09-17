package solid.humank.genaidemo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * 基本應用程式測試 - 驗證應用程式能正常啟動
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.main.lazy-initialization=true",
    "logging.level.root=ERROR",
    "management.endpoints.enabled=false",
    "management.endpoint.health.enabled=false"
})
public class BasicApplicationTest {

    @Test
    void contextLoads() {
        // 測試應用程式上下文是否能正常載入
        // 如果能到達這裡，表示 Spring Boot 應用程式能正常啟動
    }
}