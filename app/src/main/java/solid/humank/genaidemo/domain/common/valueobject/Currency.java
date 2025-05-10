package solid.humank.genaidemo.domain.common.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/**
 * 貨幣值對象
 * 
 * 表示不同的貨幣類型，包含顯示名稱、代碼和小數位數。
 * 作為值對象，它是不可變的，所有屬性在創建後不能被修改。
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
    
    /**
     * 建立貨幣
     * 
     * @param displayName 顯示名稱
     */
    Currency(String displayName) {
        this(displayName, displayName, 2);
    }
    
    /**
     * 建立貨幣
     * 
     * @param displayName 顯示名稱
     * @param code 貨幣代碼
     * @param decimalPlaces 小數位數
     */
    Currency(String displayName, String code, int decimalPlaces) {
        this.displayName = displayName;
        this.code = code;
        this.decimalPlaces = decimalPlaces;
    }
    
    /**
     * 獲取顯示名稱
     * 
     * @return 顯示名稱
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 獲取貨幣代碼
     * 
     * @return 貨幣代碼
     */
    public String getCode() {
        return code;
    }
    
    /**
     * 獲取小數位數
     * 
     * @return 小數位數
     */
    public int getDecimalPlaces() {
        return decimalPlaces;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}