package solid.humank.genaidemo.domain.order.model.valueobject.shared;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/**
 * 共享核心中的幣別值物件
 */
@ValueObject
public enum Currency {
    TWD("TWD", "新台幣", 0),
    USD("USD", "美元", 2),
    EUR("EUR", "歐元", 2),
    JPY("JPY", "日元", 0),
    CNY("CNY", "人民幣", 2);
    
    private final String code;
    private final String displayName;
    private final int decimalPlaces;
    
    Currency(String code, String displayName, int decimalPlaces) {
        this.code = code;
        this.displayName = displayName;
        this.decimalPlaces = decimalPlaces;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public int getDecimalPlaces() {
        return decimalPlaces;
    }
    
    @Override
    public String toString() {
        return code;
    }
}