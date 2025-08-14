package solid.humank.genaidemo.domain.common.valueobject;

import java.util.Objects;
import java.util.UUID;
import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 訂單ID值對象 */
@ValueObject
public class OrderId {
    private final UUID id;

    public OrderId(UUID id) {
        this.id = Objects.requireNonNull(id, "Order ID cannot be null");
    }

    /**
     * 從UUID創建訂單ID
     *
     * @param id UUID
     * @return 訂單ID
     */
    public static OrderId of(UUID id) {
        return new OrderId(id);
    }

    /**
     * 從字符串創建訂單ID
     *
     * @param id 字符串
     * @return 訂單ID
     */
    public static OrderId of(String id) {
        return new OrderId(UUID.fromString(id));
    }

    /**
     * 生成新的訂單ID
     *
     * @return 訂單ID
     */
    public static OrderId generate() {
        return new OrderId(UUID.randomUUID());
    }

    /**
     * 從UUID創建訂單ID
     *
     * @param uuid UUID
     * @return 訂單ID
     */
    public static OrderId fromUUID(UUID uuid) {
        return new OrderId(uuid);
    }

    /**
     * 從字符串創建訂單ID
     *
     * @param id 字符串
     * @return 訂單ID
     */
    public static OrderId fromString(String id) {
        return new OrderId(UUID.fromString(id));
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
