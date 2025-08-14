package solid.humank.genaidemo.domain.delivery.model.valueobject;

import java.util.Objects;
import java.util.UUID;
import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 配送ID值對象 */
@ValueObject
public class DeliveryId {
    private final UUID id;

    private DeliveryId(UUID id) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
    }

    /**
     * 生成新的配送ID
     *
     * @return 新的配送ID
     */
    public static DeliveryId generate() {
        return new DeliveryId(UUID.randomUUID());
    }

    /**
     * 從UUID創建配送ID
     *
     * @param id UUID
     * @return 配送ID
     */
    public static DeliveryId fromUUID(UUID id) {
        return new DeliveryId(id);
    }

    /**
     * 從字符串創建配送ID
     *
     * @param id 字符串ID
     * @return 配送ID
     */
    public static DeliveryId fromString(String id) {
        return new DeliveryId(UUID.fromString(id));
    }

    /**
     * 獲取UUID
     *
     * @return UUID
     */
    public UUID getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeliveryId that = (DeliveryId) o;
        return Objects.equals(id, that.id);
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
