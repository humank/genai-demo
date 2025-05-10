package solid.humank.genaidemo.domain.common.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/**
 * 貨幣值對象
 * 表示不同的貨幣類型
 */
@ValueObject
public enum Currency {
    TWD("新台幣", "TWD", 0),
    USD("美元", "USD", 2),
    EUR("歐元", "EUR", 2),
    JPY("日元", "JPY", 0),
    CNY("人民幣", "CNY", 2);
    
    private final String displayName;
    private final String code;
    private final int decimalPlaces;
    
    Currency(String displayName) {
        this(displayName, displayName, 2);
    }
    
    Currency(String displayName, String code, int decimalPlaces) {
        this.displayName = displayName;
        this.code = code;
        this.decimalPlaces = decimalPlaces;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getCode() {
        return code;
    }
    
    public int getDecimalPlaces() {
        return decimalPlaces;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}