package solid.humank.genaidemo.infrastructure.common.persistence.converter;

/**
 * JSON轉換異常
 * 
 * 需求 3.3: 限制Repository Adapter的框架依賴
 */
public class JsonConversionException extends RuntimeException {

    public JsonConversionException(String message) {
        super(message);
    }

    public JsonConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}