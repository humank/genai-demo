package solid.humank.genaidemo.domain.order.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

import java.util.Objects;

/**
 * 訂單項值對象
 */
@ValueObject
public class OrderItem {
    private final String productId;
    private final String productName;
    private final int quantity;
    private final Money price;

    public OrderItem(String productId, String productName, int quantity, Money price) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        this.productId = Objects.requireNonNull(productId, "Product ID cannot be null");
        this.productName = Objects.requireNonNull(productName, "Product name cannot be null");
        this.price = Objects.requireNonNull(price, "Price cannot be null");
        this.quantity = quantity;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public Money getPrice() {
        return price;
    }

    /**
     * 計算訂單項小計金額
     */
    public Money getSubtotal() {
        return price.multiply(quantity);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderItem orderItem = (OrderItem) o;
        return quantity == orderItem.quantity &&
                Objects.equals(productId, orderItem.productId) &&
                Objects.equals(productName, orderItem.productName) &&
                Objects.equals(price, orderItem.price);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, productName, quantity, price);
    }

    @Override
    public String toString() {
        return "OrderItem{" +
                "productId='" + productId + '\'' +
                ", productName='" + productName + '\'' +
                ", quantity=" + quantity +
                ", price=" + price +
                '}';
    }
}