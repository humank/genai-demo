package solid.humank.genaidemo.domain.customer;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 地址值對象 */
@ValueObject(name = "Address", description = "客戶地址信息")
public class Address {

    private final String value;

    public Address(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("地址不能為空");
        }
        if (value.trim().length() > 200) {
            throw new IllegalArgumentException("地址不能超過200個字符");
        }
        this.value = value.trim();
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Address address = (Address) obj;
        return value.equals(address.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
