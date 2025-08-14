package solid.humank.genaidemo.domain.customer.model.valueobject;

import java.util.UUID;
import solid.humank.genaidemo.domain.common.annotations.ValueObject;

@ValueObject
public class CustomerId {
    private final String id;

    public CustomerId() {
        this.id = UUID.randomUUID().toString();
    }

    public CustomerId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomerId customerId = (CustomerId) o;
        return id.equals(customerId.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
