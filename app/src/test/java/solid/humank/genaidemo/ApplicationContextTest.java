package solid.humank.genaidemo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import solid.humank.genaidemo.testutils.BaseTest;

@SpringBootTest
@ActiveProfiles("test")
public class ApplicationContextTest extends BaseTest {

    @Test
    void contextLoads() {
        // 這個測試只是檢查 Spring ApplicationContext 是否能正常啟動
    }
}
