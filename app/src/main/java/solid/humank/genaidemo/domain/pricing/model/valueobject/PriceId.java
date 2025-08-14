package solid.humank.genaidemo.domain.pricing.model.valueobject;

import java.util.UUID;
import solid.humank.genaidemo.domain.common.annotations.ValueObject;

@ValueObject
public class PriceId {
    private final String id;

    public PriceId() {
        this.id = UUID.randomUUID().toString();
    }

    public PriceId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PriceId priceId = (PriceId) o;
        return id.equals(priceId.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
