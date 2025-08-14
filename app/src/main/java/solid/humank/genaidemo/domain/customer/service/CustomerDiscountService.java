package solid.humank.genaidemo.domain.customer.service;

import solid.humank.genaidemo.domain.common.annotations.DomainService;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.customer.model.aggregate.Customer;
import solid.humank.genaidemo.domain.order.model.aggregate.Order;

/**
 * 客戶折扣服務
 */
@DomainService(description = "客戶折扣服務，處理客戶相關的折扣邏輯和會員優惠")
public class CustomerDiscountService {
    
    private static final int NEW_MEMBER_DISCOUNT_PERCENTAGE = 15;
    private static final int BIRTHDAY_DISCOUNT_PERCENTAGE = 10;
    private static final int BIRTHDAY_DISCOUNT_CAP = 100;
    
    /**
     * 檢查客戶是否為新會員（註冊30天內）
     */
    public boolean isNewMember(Customer customer) {
        return customer.isNewMember();
    }
    
    /**
     * 檢查客戶是否在生日月份
     */
    public boolean isBirthdayMonth(Customer customer) {
        return customer.isBirthdayMonth();
    }
    
    /**
     * 檢查客戶是否沒有之前的購買記錄
     */
    public boolean hasNoPreviousPurchases(Customer customer) {
        // 實際實現中，這裡應該查詢訂單歷史
        return true;
    }
    
    /**
     * 獲取新會員折扣百分比
     */
    public int getNewMemberDiscountPercentage() {
        return NEW_MEMBER_DISCOUNT_PERCENTAGE;
    }
    
    /**
     * 獲取生日月份折扣百分比
     */
    public int getBirthdayDiscountPercentage() {
        return BIRTHDAY_DISCOUNT_PERCENTAGE;
    }
    
    /**
     * 應用新會員折扣
     */
    public Money applyNewMemberDiscount(Order order) {
        Money totalAmount = order.getTotalAmount();
        int discountAmount = totalAmount.getAmount().intValue() * NEW_MEMBER_DISCOUNT_PERCENTAGE / 100;
        return totalAmount.subtract(Money.twd(discountAmount));
    }
    
    /**
     * 應用生日月份折扣
     */
    public Money applyBirthdayDiscount(Order order) {
        Money totalAmount = order.getTotalAmount();
        int discountAmount = totalAmount.getAmount().intValue() * BIRTHDAY_DISCOUNT_PERCENTAGE / 100;
        // 生日折扣有上限
        discountAmount = Math.min(discountAmount, BIRTHDAY_DISCOUNT_CAP);
        return totalAmount.subtract(Money.twd(discountAmount));
    }
    
    /**
     * 應用最佳折扣
     */
    public Money applyBestDiscount(Order order, Customer customer) {
        if (isNewMember(customer) && isBirthdayMonth(customer)) {
            // 如果同時符合新會員和生日月份，選擇較高的折扣
            if (NEW_MEMBER_DISCOUNT_PERCENTAGE > BIRTHDAY_DISCOUNT_PERCENTAGE) {
                return applyNewMemberDiscount(order);
            } else {
                return applyBirthdayDiscount(order);
            }
        } else if (isNewMember(customer)) {
            return applyNewMemberDiscount(order);
        } else if (isBirthdayMonth(customer)) {
            return applyBirthdayDiscount(order);
        } else {
            return order.getTotalAmount();
        }
    }
}