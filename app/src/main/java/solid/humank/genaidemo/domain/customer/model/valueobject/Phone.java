package solid.humank.genaidemo.domain.customer.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

@ValueObject
public class Phone {
    private final String phone;

    public Phone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be null or empty");
        }
        this.phone = phone.trim();
    }

    public String getPhone() {
        return phone;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Phone that = (Phone) o;
        return phone.equals(that.phone);
    }

    @Override
    public int hashCode() {
        return phone.hashCode();
    }

    @Override
    public String toString() {
        return phone;
    }
}