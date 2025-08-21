package solid.humank.genaidemo.infrastructure.common.persistence.converter;

/**
 * JSON轉換器接口
 * 提供JSON序列化和反序列化的抽象
 * 
 * 需求 3.3: 限制Repository Adapter的框架依賴
 */
public interface JsonConverter {

    /**
     * 將對象序列化為JSON字符串
     */
    String toJson(Object object);

    /**
     * 將JSON字符串反序列化為指定類型的對象
     */
    <T> T fromJson(String json, Class<T> type);
}