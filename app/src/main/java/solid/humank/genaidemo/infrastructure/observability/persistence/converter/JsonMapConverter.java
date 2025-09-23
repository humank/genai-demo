package solid.humank.genaidemo.infrastructure.observability.persistence.converter;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JSON Map 轉換器
 * 
 * 用於將 Map<String, Object> 轉換為 JSON 字符串儲存到資料庫中。
 * 支援可觀測性事件數據的靈活儲存。
 * 
 * 設計原則：
 * - 自動轉換，無需手動處理
 * - 支援複雜的嵌套數據結構
 * - 錯誤處理和日誌記錄
 * - 效能優化的 ObjectMapper 配置
 */
@Converter(autoApply = false)
public class JsonMapConverter implements AttributeConverter<Map<String, Object>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting map to JSON string", e);
        }
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return Map.of();
        }

        try {
            return objectMapper.readValue(dbData, typeReference);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting JSON string to map", e);
        }
    }
}