package solid.humank.genaidemo.domain.product.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 庫存數量值對象 - 使用 Record 實作 */
@ValueObject
public record StockQuantity(int value) {

    /**
     * 緊湊建構子 - 驗證參數
     */
    public StockQuantity {
        if (value < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative");
        }
    }

    /**
     * 獲取數量值（向後相容方法）
     *
     * @return 庫存數量
     */
    public int getValue() {
        return value;
    }

    /**
     * 檢查是否有庫存
     *
     * @return 是否有庫存
     */
    public boolean isAvailable() {
        return value > 0;
    }

    /**
     * 減少庫存
     *
     * @param quantity 減少的數量
     * @return 新的庫存數量對象
     */
    public StockQuantity subtract(int quantity) {
        return new StockQuantity(this.value - quantity);
    }

    /**
     * 增加庫存
     *
     * @param quantity 增加的數量
     * @return 新的庫存數量對象
     */
    public StockQuantity add(int quantity) {
        return new StockQuantity(this.value + quantity);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
