package solid.humank.genaidemo.domain.inventory.model.valueobject;

import java.util.Objects;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 庫存預留ID值對象 - 使用 Record 實作 */
@ValueObject
public record ReservationId(UUID id) {

    /**
     * 緊湊建構子 - 驗證參數
     */
    public ReservationId {
        Objects.requireNonNull(id, "ID cannot be null");
    }

    /**
     * 創建新的預留ID
     *
     * @return 新的預留ID
     */
    public static ReservationId create() {
        return new ReservationId(UUID.randomUUID());
    }

    /**
     * 從UUID創建預留ID
     *
     * @param id UUID
     * @return 預留ID
     */
    public static ReservationId fromUUID(UUID id) {
        return new ReservationId(id);
    }

    /**
     * 從字符串創建預留ID
     *
     * @param id 字符串ID
     * @return 預留ID
     */
    public static ReservationId fromString(String id) {
        return new ReservationId(UUID.fromString(id));
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
