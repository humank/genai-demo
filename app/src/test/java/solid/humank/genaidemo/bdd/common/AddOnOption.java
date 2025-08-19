package solid.humank.genaidemo.bdd.common;

import java.math.BigDecimal;

/** 加購選項 - 用於 BDD 測試 */
public class AddOnOption {
    private String productId;
    private String productName;
    private BigDecimal originalPrice;
    private BigDecimal addOnPrice;
    private BigDecimal savings;
    private int limitQuantity;
    private boolean available;

    public AddOnOption(
            String productId,
            String productName,
            BigDecimal originalPrice,
            BigDecimal addOnPrice,
            int limitQuantity) {
        this.productId = productId;
        this.productName = productName;
        this.originalPrice = originalPrice;
        this.addOnPrice = addOnPrice;
        this.savings = originalPrice.subtract(addOnPrice);
        this.limitQuantity = limitQuantity;
        this.available = true;
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

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
        this.savings = originalPrice.subtract(addOnPrice);
    }

    public BigDecimal getAddOnPrice() {
        return addOnPrice;
    }

    public void setAddOnPrice(BigDecimal addOnPrice) {
        this.addOnPrice = addOnPrice;
        this.savings = originalPrice.subtract(addOnPrice);
    }

    public BigDecimal getSavings() {
        return savings;
    }

    public int getLimitQuantity() {
        return limitQuantity;
    }

    public void setLimitQuantity(int limitQuantity) {
        this.limitQuantity = limitQuantity;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
