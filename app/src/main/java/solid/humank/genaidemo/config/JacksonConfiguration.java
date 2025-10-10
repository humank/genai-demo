package solid.humank.genaidemo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson Configuration for proper JSON serialization/deserialization
 * 
 * This configuration ensures that Java 8 time types (LocalDateTime, Instant, etc.)
 * are properly handled by Jackson ObjectMapper.
 * 
 * @author Development Team
 * @since 2.0.0
 */
@Configuration
public class JacksonConfiguration {

    /**
     * Configure ObjectMapper with JSR310 support for Java 8 time types
     * 
     * @return configured ObjectMapper
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Register JSR310 module for Java 8 time support
        mapper.registerModule(new JavaTimeModule());
        
        // Disable writing dates as timestamps
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        return mapper;
    }
}