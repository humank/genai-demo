package solid.humank.genaidemo.infrastructure.common.persistence.converter;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Jackson實現的JSON轉換器
 * 將Jackson框架依賴封裝在基礎設施層
 * 
 * 需求 3.3: 限制Repository Adapter的框架依賴
 */
@Component
public class JacksonJsonConverter implements JsonConverter {

    private final ObjectMapper objectMapper;

    public JacksonJsonConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new JsonConversionException("Failed to serialize object to JSON", e);
        }
    }

    @Override
    public <T> T fromJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new JsonConversionException("Failed to deserialize JSON to object", e);
        }
    }
}