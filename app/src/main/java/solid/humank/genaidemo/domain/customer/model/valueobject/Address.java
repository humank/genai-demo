package solid.humank.genaidemo.domain.customer.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

@ValueObject
public class Address {
    private final String street;
    private final String city;
    private final String zipCode;
    private final String country;

    public Address(String street, String city, String zipCode, String country) {
        this.street = street;
        this.city = city;
        this.zipCode = zipCode;
        this.country = country;
    }

    public String getStreet() {
        return street;
    }

    public String getCity() {
        return city;
    }

    public String getZipCode() {
        return zipCode;
    }

    public String getCountry() {
        return country;
    }

    public String getFullAddress() {
        return toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Address address = (Address) o;
        return java.util.Objects.equals(street, address.street)
                && java.util.Objects.equals(city, address.city)
                && java.util.Objects.equals(zipCode, address.zipCode)
                && java.util.Objects.equals(country, address.country);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(street, city, zipCode, country);
    }

    @Override
    public String toString() {
        return street + ", " + city + " " + zipCode + ", " + country;
    }
}
