package solid.humank.genaidemo.domain.customer.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

@ValueObject
public class CustomerName {
    private final String name;

    public CustomerName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name cannot be null or empty");
        }
        this.name = name.trim();
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomerName that = (CustomerName) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}