package solid.humank.genaidemo.bdd.common;

import java.math.BigDecimal;

/** 購物車項目 - 用於 BDD 測試 */
public class CartItem {
    private String productId;
    private String productName;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private int quantity;
    private boolean isAddOn;
    private String type; // "Main" or "Add-on"

    public CartItem(String productId, String productName, BigDecimal price, int quantity) {
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.originalPrice = price;
        this.quantity = quantity;
        this.isAddOn = false;
        this.type = "Main";
    }

    public CartItem(
            String productId,
            String productName,
            BigDecimal price,
            BigDecimal originalPrice,
            int quantity,
            boolean isAddOn) {
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.originalPrice = originalPrice;
        this.quantity = quantity;
        this.isAddOn = isAddOn;
        this.type = isAddOn ? "Add-on" : "Main";
    }

    // Getters and Setters
    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public boolean isAddOn() {
        return isAddOn;
    }

    public void setAddOn(boolean addOn) {
        isAddOn = addOn;
        this.type = addOn ? "Add-on" : "Main";
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
        this.isAddOn = "Add-on".equals(type);
    }

    public BigDecimal getTotalPrice() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    public BigDecimal getSavings() {
        if (isAddOn && originalPrice != null) {
            return originalPrice.subtract(price).multiply(BigDecimal.valueOf(quantity));
        }
        return BigDecimal.ZERO;
    }
}
