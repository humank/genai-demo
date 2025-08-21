package solid.humank.genaidemo.domain.customer.model.valueobject;

import java.util.Objects;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 地址值對象 - 使用 Record 實作 */
@ValueObject
public record Address(String street, String city, String zipCode, String country) {

    /**
     * 緊湊建構子 - 驗證參數
     */
    public Address {
        Objects.requireNonNull(street, "Street cannot be null");
        Objects.requireNonNull(city, "City cannot be null");
        Objects.requireNonNull(zipCode, "Zip code cannot be null");
        Objects.requireNonNull(country, "Country cannot be null");

        // 正規化：去除前後空白
        street = street.trim();
        city = city.trim();
        zipCode = zipCode.trim();
        country = country.trim();

        if (street.isEmpty()) {
            throw new IllegalArgumentException("Street cannot be empty");
        }
        if (city.isEmpty()) {
            throw new IllegalArgumentException("City cannot be empty");
        }
        if (zipCode.isEmpty()) {
            throw new IllegalArgumentException("Zip code cannot be empty");
        }
        if (country.isEmpty()) {
            throw new IllegalArgumentException("Country cannot be empty");
        }
    }

    /**
     * 獲取街道（向後相容方法）
     *
     * @return 街道
     */
    public String getStreet() {
        return street;
    }

    /**
     * 獲取城市（向後相容方法）
     *
     * @return 城市
     */
    public String getCity() {
        return city;
    }

    /**
     * 獲取郵遞區號（向後相容方法）
     *
     * @return 郵遞區號
     */
    public String getZipCode() {
        return zipCode;
    }

    /**
     * 獲取國家（向後相容方法）
     *
     * @return 國家
     */
    public String getCountry() {
        return country;
    }

    /**
     * 獲取完整地址
     *
     * @return 完整地址字符串
     */
    public String getFullAddress() {
        return toString();
    }

    @Override
    public String toString() {
        return street + ", " + city + " " + zipCode + ", " + country;
    }
}
