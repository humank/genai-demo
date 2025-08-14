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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Address address = (Address) o;
        return street.equals(address.street)
                && city.equals(address.city)
                && zipCode.equals(address.zipCode)
                && country.equals(address.country);
    }

    @Override
    public int hashCode() {
        return street.hashCode() + city.hashCode() + zipCode.hashCode() + country.hashCode();
    }

    @Override
    public String toString() {
        return street + ", " + city + " " + zipCode + ", " + country;
    }
}
