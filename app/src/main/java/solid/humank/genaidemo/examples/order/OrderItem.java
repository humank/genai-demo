package solid.humank.genaidemo.examples.order;

import solid.humank.genaidemo.ddd.annotations.Entity;

@Entity
public record OrderItem(
    String productId,
    String productName,
    int quantity,
    Money unitPrice
) {
    public OrderItem {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("商品ID不能為空");
        }
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("商品名稱不能為空");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("數量必須大於0");
        }
        if (unitPrice == null) {
            throw new IllegalArgumentException("單價不能為空");
        }
    }

    public Money getSubtotal() {
        return unitPrice.multiply(quantity);
    }

    @Override
    public String toString() {
        return String.format(
            "%s (數量: %d, 單價: %s, 小計: %s)",
            productName,
            quantity,
            unitPrice,
            getSubtotal()
        );
    }
}
