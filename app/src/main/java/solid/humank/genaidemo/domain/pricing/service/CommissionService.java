package solid.humank.genaidemo.domain.pricing.service;

import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.pricing.model.entity.CommissionRate;
import solid.humank.genaidemo.domain.pricing.model.valueobject.ProductCategory;
import solid.humank.genaidemo.domain.product.model.aggregate.Product;

/**
 * 佣金服務
 */
public class CommissionService {
    
    /**
     * 獲取指定產品類別的一般佣金費率
     */
    public CommissionRate getCommissionRate(ProductCategory category) {
        // 實際實現中，這裡應該從資料庫或配置中獲取費率
        return new CommissionRate(category, getDefaultNormalRate(category), getDefaultEventRate(category));
    }
    
    /**
     * 獲取指定產品類別和活動的佣金費率
     */
    public CommissionRate getCommissionRate(ProductCategory category, String event) {
        // 實際實現中，這裡應該從資料庫或配置中獲取特定活動的費率
        CommissionRate rate = getCommissionRate(category);
        // 如果是特定活動，可能會有不同的費率
        return rate;
    }
    
    /**
     * 計算佣金金額
     */
    public Money calculateCommission(Product product, Money salePrice, String event) {
        ProductCategory category = product.getCategory();
        CommissionRate rate = getCommissionRate(category, event);
        
        // 根據活動類型決定使用哪種費率
        int ratePercentage = event != null ? rate.getEventRate() : rate.getNormalRate();
        
        // 計算佣金金額
        return salePrice.multiply(ratePercentage / 100.0);
    }
    
    /**
     * 通知賣家費率變更
     */
    public void notifySeller(Product product, String event, int days) {
        // 實際實現中，這裡應該發送通知給賣家
    }
    
    // 獲取預設的一般費率
    private int getDefaultNormalRate(ProductCategory category) {
        switch (category) {
            case ELECTRONICS:
                return 3;
            case FASHION:
                return 5;
            case GROCERIES:
                return 2;
            default:
                return 4;
        }
    }
    
    // 獲取預設的活動費率
    private int getDefaultEventRate(ProductCategory category) {
        switch (category) {
            case ELECTRONICS:
                return 5;
            case FASHION:
                return 8;
            case GROCERIES:
                return 3;
            default:
                return 6;
        }
    }
}