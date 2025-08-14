package solid.humank.genaidemo.domain.product;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 庫存數量值對象 */
@ValueObject(name = "StockQuantity", description = "產品庫存數量")
public class StockQuantity {

    private final int value;

    public StockQuantity(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("庫存數量不能為負數");
        }
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public boolean isAvailable() {
        return value > 0;
    }

    public StockQuantity subtract(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("扣減數量不能為負數");
        }
        if (quantity > value) {
            throw new IllegalArgumentException("庫存不足，無法扣減");
        }
        return new StockQuantity(value - quantity);
    }

    public StockQuantity add(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("增加數量不能為負數");
        }
        return new StockQuantity(value + quantity);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        StockQuantity that = (StockQuantity) obj;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
