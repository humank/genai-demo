package solid.humank.genaidemo.domain.inventory.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

import java.util.Objects;
import java.util.UUID;

/**
 * 庫存ID值對象
 */
@ValueObject
public class InventoryId {
    private final UUID id;

    private InventoryId(UUID id) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
    }

    /**
     * 生成新的庫存ID
     *
     * @return 新的庫存ID
     */
    public static InventoryId generate() {
        return new InventoryId(UUID.randomUUID());
    }

    /**
     * 從UUID創建庫存ID
     *
     * @param id UUID
     * @return 庫存ID
     */
    public static InventoryId fromUUID(UUID id) {
        return new InventoryId(id);
    }

    /**
     * 從字符串創建庫存ID
     *
     * @param id 字符串ID
     * @return 庫存ID
     */
    public static InventoryId fromString(String id) {
        return new InventoryId(UUID.fromString(id));
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
        InventoryId that = (InventoryId) o;
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