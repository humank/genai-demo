package solid.humank.genaidemo.domain.seller.model.valueobject;

import java.util.Objects;
import java.util.UUID;
import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 賣家ID值物件 */
@ValueObject
public class SellerId {
    private final String id;

    public SellerId(String id) {
        this.id = id;
    }

    public static SellerId generateNew() {
        return new SellerId(UUID.randomUUID().toString());
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SellerId sellerId = (SellerId) o;
        return Objects.equals(id, sellerId.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return id;
    }
}
