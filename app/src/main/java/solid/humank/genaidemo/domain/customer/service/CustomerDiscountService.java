package solid.humank.genaidemo.domain.customer.service;

import solid.humank.genaidemo.domain.common.annotations.DomainService;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.customer.model.aggregate.Customer;

/** 客戶折扣服務 */
@DomainService(name = "CustomerDiscountService", description = "客戶折扣服務，處理客戶相關的折扣邏輯和會員優惠", boundedContext = "Customer")
public class CustomerDiscountService {

    private static final int NEW_MEMBER_DISCOUNT_PERCENTAGE = 15;
    private static final int BIRTHDAY_DISCOUNT_PERCENTAGE = 10;
    private static final int BIRTHDAY_DISCOUNT_CAP = 100;

    /** 檢查客戶是否為新會員（註冊30天內） */
    public boolean isNewMember(Customer customer) {
        return customer.isNewMember();
    }

    /** 檢查客戶是否在生日月份 */
    public boolean isBirthdayMonth(Customer customer) {
        return customer.isBirthdayMonth();
    }

    /** 檢查客戶是否沒有之前的購買記錄 */
    public boolean hasNoPreviousPurchases(Customer customer) {
        // 實際實現中，這裡應該查詢訂單歷史
        return true;
    }

    /** 獲取新會員折扣百分比 */
    public int getNewMemberDiscountPercentage() {
        return NEW_MEMBER_DISCOUNT_PERCENTAGE;
    }

    /** 獲取生日月份折扣百分比 */
    public int getBirthdayDiscountPercentage() {
        return BIRTHDAY_DISCOUNT_PERCENTAGE;
    }

    /** 計算新會員折扣金額 */
    public Money calculateNewMemberDiscount(Money totalAmount) {
        int discountAmount = totalAmount.getAmount().intValue() * NEW_MEMBER_DISCOUNT_PERCENTAGE / 100;
        return Money.twd(discountAmount);
    }

    /** 計算生日月份折扣金額 */
    public Money calculateBirthdayDiscount(Money totalAmount) {
        int discountAmount = totalAmount.getAmount().intValue() * BIRTHDAY_DISCOUNT_PERCENTAGE / 100;
        // 生日折扣有上限
        discountAmount = Math.min(discountAmount, BIRTHDAY_DISCOUNT_CAP);
        return Money.twd(discountAmount);
    }

    /** 計算客戶可獲得的最佳折扣金額 */
    public Money calculateBestDiscount(Money totalAmount, Customer customer) {
        Money bestDiscount = Money.twd(0);

        // 檢查新會員折扣
        if (isNewMember(customer)) {
            Money newMemberDiscount = calculateNewMemberDiscount(totalAmount);
            if (newMemberDiscount.getAmount().compareTo(bestDiscount.getAmount()) > 0) {
                bestDiscount = newMemberDiscount;
            }
        }

        // 檢查生日月份折扣
        if (isBirthdayMonth(customer)) {
            Money birthdayDiscount = calculateBirthdayDiscount(totalAmount);
            if (birthdayDiscount.getAmount().compareTo(bestDiscount.getAmount()) > 0) {
                bestDiscount = birthdayDiscount;
            }
        }

        return bestDiscount;
    }
}
