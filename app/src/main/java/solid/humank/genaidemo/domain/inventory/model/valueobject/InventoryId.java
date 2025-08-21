package solid.humank.genaidemo.domain.inventory.model.valueobject;

import java.util.Objects;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 庫存ID值對象 - 使用 Record 實作 */
@ValueObject
public record InventoryId(UUID id) {

    /**
     * 緊湊建構子 - 驗證參數
     */
    public InventoryId {
        Objects.requireNonNull(id, "ID cannot be null");
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
     * 獲取UUID（向後相容方法）
     *
     * @return UUID
     */
    public UUID getId() {
        return id;
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
