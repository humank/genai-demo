package solid.humank.genaidemo.domain.common.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

import java.util.Objects;

/**
 * 訂單項值對象
 * 
 * 訂單項是訂單中的基本組成單位，代表一個產品的購買數量和價格。
 * 作為值對象，它是不可變的，所有屬性在創建後不能被修改。
 */
@ValueObject
public class OrderItem {
    private final String productId;
    private final String productName;
    private final int quantity;
    private final Money price;

    /**
     * 建立訂單項
     * 
     * @param productId 產品ID
     * @param productName 產品名稱
     * @param quantity 數量
     * @param price 價格
     */
    public OrderItem(String productId, String productName, int quantity, Money price) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        this.productId = Objects.requireNonNull(productId, "Product ID cannot be null");
        this.productName = Objects.requireNonNull(productName, "Product name cannot be null");
        this.price = Objects.requireNonNull(price, "Price cannot be null");
        this.quantity = quantity;
    }

    /**
     * 獲取產品ID
     * 
     * @return 產品ID
     */
    public String getProductId() {
        return productId;
    }

    /**
     * 獲取產品名稱
     * 
     * @return 產品名稱
     */
    public String getProductName() {
        return productName;
    }

    /**
     * 獲取數量
     * 
     * @return 數量
     */
    public int getQuantity() {
        return quantity;
    }

    /**
     * 獲取價格
     * 
     * @return 價格
     */
    public Money getPrice() {
        return price;
    }

    /**
     * 計算訂單項小計金額
     * 
     * @return 小計金額
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