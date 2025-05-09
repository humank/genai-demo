package solid.humank.genaidemo.domain.common.valueobject;

/**
 * 共享核心中的貨幣列舉
 * 用於在不同 Bounded Context 之間共享貨幣類型
 */
public enum Currency {
    TWD("TWD", "新台幣", 2),
    USD("USD", "美元", 2),
    JPY("JPY", "日圓", 0),
    EUR("EUR", "歐元", 2);

    private final String code;
    private final String name;
    private final int decimalPlaces;

    Currency(String code, String name, int decimalPlaces) {
        this.code = code;
        this.name = name;
        this.decimalPlaces = decimalPlaces;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public int getDecimalPlaces() {
        return decimalPlaces;
    }

    public static Currency fromCode(String code) {
        for (Currency currency : values()) {
            if (currency.code.equals(code)) {
                return currency;
            }
        }
        throw new IllegalArgumentException("Unknown currency code: " + code);
    }
}
