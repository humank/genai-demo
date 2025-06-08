package solid.humank.genaidemo.domain.pricing.model.entity;

import solid.humank.genaidemo.domain.pricing.model.valueobject.ProductCategory;

/**
 * 佣金費率實體
 */
public class CommissionRate {
    private ProductCategory category;
    private int normalRate; // 一般費率（百分比）
    private int eventRate;  // 活動費率（百分比）

    public CommissionRate(ProductCategory category, int normalRate, int eventRate) {
        this.category = category;
        this.normalRate = normalRate;
        this.eventRate = eventRate;
    }
    
    /**
     * 建立佣金費率（不指定產品類別）
     * 
     * @param normalRate 一般費率
     * @param eventRate 活動費率
     */
    public CommissionRate(int normalRate, int eventRate) {
        this.category = ProductCategory.GENERAL;
        this.normalRate = normalRate;
        this.eventRate = eventRate;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public int getNormalRate() {
        return normalRate;
    }

    public void setNormalRate(int normalRate) {
        this.normalRate = normalRate;
    }

    public int getEventRate() {
        return eventRate;
    }

    public void setEventRate(int eventRate) {
        this.eventRate = eventRate;
    }
}