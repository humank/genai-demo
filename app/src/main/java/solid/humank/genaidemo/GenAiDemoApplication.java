package solid.humank.genaidemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import solid.humank.genaidemo.config.OrderProperties;

@SpringBootApplication
@EnableConfigurationProperties(OrderProperties.class)
public class GenAiDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(GenAiDemoApplication.class, args);
    }
}
