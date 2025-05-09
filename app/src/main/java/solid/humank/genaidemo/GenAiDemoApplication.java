package solid.humank.genaidemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 應用程式入口點
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "solid.humank.genaidemo.domain",
    "solid.humank.genaidemo.application",
    "solid.humank.genaidemo.infrastructure",
    "solid.humank.genaidemo.interfaces"
})
public class GenAiDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(GenAiDemoApplication.class, args);
    }
}