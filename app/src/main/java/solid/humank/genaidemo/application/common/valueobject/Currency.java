package solid.humank.genaidemo.application.common.valueobject;

/**
 * 貨幣單位值對象 (應用層)
 * 
 * 表示貨幣單位。
 * 作為應用層的值對象，它是領域層值對象的簡化版本。
 */
public enum Currency {
    TWD("新台幣"),
    USD("美元"),
    EUR("歐元"),
    JPY("日元"),
    CNY("人民幣");

    private final String description;

    /**
     * 建立貨幣單位
     * 
     * @param description 貨幣描述
     */
    Currency(String description) {
        this.description = description;
    }

    /**
     * 獲取貨幣描述
     * 
     * @return 貨幣描述
     */
    public String getDescription() {
        return description;
    }
}