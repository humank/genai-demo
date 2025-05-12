package solid.humank.genaidemo.domain.inventory.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

import java.util.Objects;
import java.util.UUID;

/**
 * 庫存預留ID值對象
 */
@ValueObject
public class ReservationId {
    private final UUID id;

    private ReservationId(UUID id) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
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
        ReservationId that = (ReservationId) o;
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