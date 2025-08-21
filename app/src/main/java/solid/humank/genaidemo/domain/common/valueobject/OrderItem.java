package solid.humank.genaidemo.domain.common.valueobject;

import java.util.Objects;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/**
 * 訂單項值對象 - 使用 Record 實作
 *
 * <p>
 * 訂單項是訂單中的基本組成單位，代表一個產品的購買數量和價格。 作為值對象，它是不可變的，所有屬性在創建後不能被修改。
 */
@ValueObject
public record OrderItem(String productId, String productName, int quantity, Money price) {

    /**
     * 緊湊建構子 - 驗證參數
     */
    public OrderItem {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        Objects.requireNonNull(productId, "Product ID cannot be null");
        Objects.requireNonNull(productName, "Product name cannot be null");
        Objects.requireNonNull(price, "Price cannot be null");
    }

    /**
     * 獲取產品ID（向後相容方法）
     *
     * @return 產品ID
     */
    public String getProductId() {
        return productId;
    }

    /**
     * 獲取產品名稱（向後相容方法）
     *
     * @return 產品名稱
     */
    public String getProductName() {
        return productName;
    }

    /**
     * 獲取數量（向後相容方法）
     *
     * @return 數量
     */
    public int getQuantity() {
        return quantity;
    }

    /**
     * 獲取價格（向後相容方法）
     *
     * @return 價格
     */
    public Money getPrice() {
        return price;
    }

    /**
     * 獲取單價 (別名方法，與 getPrice 相同)
     *
     * @return 單價
     */
    public Money getUnitPrice() {
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
    public String toString() {
        return "OrderItem{"
                + "productId='"
                + productId
                + '\''
                + ", productName='"
                + productName
                + '\''
                + ", quantity="
                + quantity
                + ", price="
                + price
                + '}';
    }
}
