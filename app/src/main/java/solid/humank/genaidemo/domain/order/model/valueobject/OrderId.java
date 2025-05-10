package solid.humank.genaidemo.domain.order.model.valueobject;

import java.util.Objects;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/**
 * 訂單ID值對象
 * 不可變的訂單唯一標識
 */
@ValueObject
public class OrderId {
    private final UUID id;
    
    /**
     * 建立訂單ID
     */
    public OrderId() {
        this(UUID.randomUUID());
    }
    
    /**
     * 建立訂單ID
     * 
     * @param id UUID
     */
    public OrderId(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }
        this.id = id;
    }
    
    /**
     * 從字串建立訂單ID
     * 
     * @param id ID字串
     * @return 訂單ID
     */
    public static OrderId fromString(String id) {
        return new OrderId(UUID.fromString(id));
    }
    
    /**
     * 從字串建立訂單ID
     * 
     * @param id ID字串
     * @return 訂單ID
     */
    public static OrderId of(String id) {
        return fromString(id);
    }
    
    /**
     * 生成新的訂單ID
     * 
     * @return 訂單ID
     */
    public static OrderId generate() {
        return new OrderId();
    }
    
    /**
     * 獲取ID
     * 
     * @return UUID
     */
    public UUID getId() {
        return id;
    }
    
    /**
     * 獲取ID值
     * 
     * @return ID字串
     */
    public String getValue() {
        return id.toString();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderId orderId = (OrderId) o;
        return Objects.equals(id, orderId.id);
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