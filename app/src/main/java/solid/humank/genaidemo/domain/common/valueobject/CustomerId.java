package solid.humank.genaidemo.domain.common.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

import java.util.Objects;
import java.util.UUID;

/**
 * 客戶ID值對象
 */
@ValueObject
public class CustomerId {
    private final UUID id;
    
    public CustomerId(UUID id) {
        this.id = Objects.requireNonNull(id, "Customer ID cannot be null");
    }
    
    /**
     * 從UUID創建客戶ID
     * 
     * @param id UUID
     * @return 客戶ID
     */
    public static CustomerId of(UUID id) {
        return new CustomerId(id);
    }
    
    /**
     * 從字符串創建客戶ID
     * 
     * @param id 字符串
     * @return 客戶ID
     */
    public static CustomerId of(String id) {
        return new CustomerId(UUID.fromString(id));
    }
    
    /**
     * 生成新的客戶ID
     * 
     * @return 客戶ID
     */
    public static CustomerId generate() {
        return new CustomerId(UUID.randomUUID());
    }
    
    /**
     * 從UUID創建客戶ID
     * 
     * @param uuid UUID
     * @return 客戶ID
     */
    public static CustomerId fromUUID(UUID uuid) {
        return new CustomerId(uuid);
    }
    
    /**
     * 從字符串創建客戶ID
     * 
     * @param id 字符串
     * @return 客戶ID
     */
    public static CustomerId fromString(String id) {
        return new CustomerId(UUID.fromString(id));
    }
    
    /**
     * 獲取ID
     * 
     * @return ID
     */
    public UUID getId() {
        return id;
    }
    
    /**
     * 獲取ID值
     * 
     * @return ID值
     */
    public String getValue() {
        return id.toString();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomerId customerId = (CustomerId) o;
        return Objects.equals(id, customerId.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return id.toString();
    }
}