package solid.humank.genaidemo.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Test Web MVC Configuration
 * 
 * Provides necessary beans for SpringDoc integration in test environment.
 */
@TestConfiguration
public class TestWebMvcConfiguration implements WebMvcConfigurer {

    /**
     * Provides the mvcConversionService bean that SpringDoc requires
     */
    @Bean(name = "mvcConversionService")
    @Primary
    public ConversionService mvcConversionService() {
        return new DefaultFormattingConversionService();
    }
}