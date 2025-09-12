package solid.humank.genaidemo.testutils;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 測試專用配置
 * 
 * 排除不必要的自動配置以提高測試性能
 */
@Configuration
@Profile("test-minimal")
@EnableAutoConfiguration(exclude = {
        KafkaAutoConfiguration.class,
        SecurityAutoConfiguration.class,
        WebMvcAutoConfiguration.class
})
// 通過 application-test-minimal.yml 配置排除 SpringDoc
public class TestConfiguration {

    // 這個配置類主要用於排除自動配置
    // 具體的 Bean 配置在其他地方定義

}