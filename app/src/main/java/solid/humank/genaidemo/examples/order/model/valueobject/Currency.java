package solid.humank.genaidemo.examples.order.model.valueobject;

import solid.humank.genaidemo.ddd.annotations.ValueObject;

/**
 * 貨幣枚舉
 */
@ValueObject
public enum Currency {
    TWD("新台幣"),
    USD("美元"),
    EUR("歐元"),
    JPY("日元"),
    CNY("人民幣");
    
    private final String displayName;
    
    Currency(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}