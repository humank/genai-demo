package solid.humank.genaidemo.domain.promotion.service;

import java.time.LocalDateTime;
import java.util.List;

import solid.humank.genaidemo.domain.common.annotations.DomainService;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.promotion.model.aggregate.Voucher;
import solid.humank.genaidemo.domain.promotion.model.valueobject.VoucherId;
import solid.humank.genaidemo.domain.promotion.repository.VoucherRepository;

/**
 * 優惠券領域服務
 * 處理優惠券相關的複雜業務邏輯和驗證
 */
@DomainService(name = "VoucherDomainService", description = "優惠券領域服務，處理優惠券的發放、驗證和使用邏輯", boundedContext = "Promotion")
public class VoucherDomainService {

    private final VoucherRepository voucherRepository;

    public VoucherDomainService(VoucherRepository voucherRepository) {
        this.voucherRepository = voucherRepository;
    }

    /**
     * 驗證優惠券是否可用
     * 
     * @param voucherId   優惠券ID
     * @param customerId  客戶ID
     * @param orderAmount 訂單金額
     * @return 驗證結果
     */
    public VoucherValidationResult validateVoucher(VoucherId voucherId, String customerId, Money orderAmount) {
        Voucher voucher = voucherRepository.findById(voucherId.value()).orElse(null);

        if (voucher == null) {
            return VoucherValidationResult.invalid("優惠券不存在");
        }

        if (!voucher.canUse()) {
            return VoucherValidationResult.invalid("優惠券已失效");
        }

        if (voucher.isExpired()) {
            return VoucherValidationResult.invalid("優惠券已過期");
        }

        // 簡化實現 - 實際應該檢查客戶是否有權使用此優惠券
        // if (!voucher.isApplicableToCustomer(customerId)) {
        // return VoucherValidationResult.invalid("此優惠券不適用於該客戶");
        // }

        // 簡化實現 - 實際應該檢查最低消費金額
        // if (!voucher.meetsMinimumAmount(orderAmount)) {
        // return VoucherValidationResult.invalid("訂單金額未達到優惠券使用門檻");
        // }

        return VoucherValidationResult.valid(voucher.getValue());
    }

    /**
     * 批量發放優惠券
     * 
     * @param customerIds     客戶ID列表
     * @param voucherTemplate 優惠券模板
     * @param expiryDate      過期日期
     * @return 發放成功的優惠券數量
     */
    public int batchIssueVouchers(List<String> customerIds, VoucherTemplate voucherTemplate, LocalDateTime expiryDate) {
        int successCount = 0;

        for (String customerId : customerIds) {
            try {
                Voucher voucher = createVoucherFromTemplate(voucherTemplate, customerId, expiryDate);
                voucherRepository.save(voucher);
                successCount++;
            } catch (Exception e) {
                // 記錄錯誤但繼續處理其他客戶
                // 這裡可以加入日誌記錄
            }
        }

        return successCount;
    }

    /**
     * 計算優惠券組合的最佳折扣
     * 當客戶有多張優惠券時，計算最佳使用組合
     * 
     * @param customerId  客戶ID
     * @param orderAmount 訂單金額
     * @return 最佳折扣組合
     */
    public VoucherCombinationResult findBestVoucherCombination(String customerId, Money orderAmount) {
        // 簡化實現 - 實際需要從repository查詢
        List<Voucher> availableVouchers = List.of();

        Money maxDiscount = Money.ZERO;
        List<VoucherId> bestCombination = List.of();

        // 簡化實現：只考慮單張優惠券使用
        // 實際實現可能需要更複雜的組合算法
        for (Voucher voucher : availableVouchers) {
            VoucherValidationResult result = validateVoucher(voucher.getId(), customerId, orderAmount);
            if (result.isValid() && result.getDiscountAmount().isGreaterThan(maxDiscount)) {
                maxDiscount = result.getDiscountAmount();
                bestCombination = List.of(voucher.getId());
            }
        }

        return new VoucherCombinationResult(bestCombination, maxDiscount);
    }

    /**
     * 清理過期優惠券
     * 
     * @return 清理的優惠券數量
     */
    public int cleanupExpiredVouchers() {
        // 簡化實現 - 實際需要從repository查詢過期優惠券
        List<Voucher> expiredVouchers = List.of();
        int cleanedCount = 0;

        for (Voucher voucher : expiredVouchers) {
            // 簡化實現 - 實際需要檢查是否可以刪除
            voucherRepository.delete(voucher);
            cleanedCount++;
        }

        return cleanedCount;
    }

    /**
     * 檢查優惠券使用異常
     * 
     * @param customerId 客戶ID
     * @param timeWindow 檢查時間窗口（小時）
     * @return 是否存在異常使用
     */
    public boolean detectAbnormalUsage(String customerId, int timeWindow) {
        LocalDateTime startTime = LocalDateTime.now().minusHours(timeWindow);
        // 簡化實現 - 實際需要從repository查詢使用次數
        int usageCount = 0;

        // 如果在短時間內使用過多優惠券，可能存在異常
        return usageCount > 10; // 可配置的閾值
    }

    // 私有輔助方法
    private Voucher createVoucherFromTemplate(VoucherTemplate template, String customerId, LocalDateTime expiryDate) {
        // 根據模板創建優惠券的邏輯
        // 這裡是簡化實現
        return null; // 實際實現需要根據模板創建具體的優惠券
    }

    /**
     * 優惠券驗證結果
     */
    public static class VoucherValidationResult {
        private final boolean valid;
        private final String errorMessage;
        private final Money discountAmount;

        private VoucherValidationResult(boolean valid, String errorMessage, Money discountAmount) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.discountAmount = discountAmount;
        }

        public static VoucherValidationResult valid(Money discountAmount) {
            return new VoucherValidationResult(true, null, discountAmount);
        }

        public static VoucherValidationResult invalid(String errorMessage) {
            return new VoucherValidationResult(false, errorMessage, Money.ZERO);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public Money getDiscountAmount() {
            return discountAmount;
        }
    }

    /**
     * 優惠券組合結果
     */
    public static class VoucherCombinationResult {
        private final List<VoucherId> voucherIds;
        private final Money totalDiscount;

        public VoucherCombinationResult(List<VoucherId> voucherIds, Money totalDiscount) {
            this.voucherIds = voucherIds;
            this.totalDiscount = totalDiscount;
        }

        public List<VoucherId> getVoucherIds() {
            return voucherIds;
        }

        public Money getTotalDiscount() {
            return totalDiscount;
        }
    }

    /**
     * 優惠券模板（簡化版）
     */
    public static class VoucherTemplate {
        private final String name;
        private final Money discountAmount;
        private final Money minimumAmount;

        public VoucherTemplate(String name, Money discountAmount, Money minimumAmount) {
            this.name = name;
            this.discountAmount = discountAmount;
            this.minimumAmount = minimumAmount;
        }

        public String getName() {
            return name;
        }

        public Money getDiscountAmount() {
            return discountAmount;
        }

        public Money getMinimumAmount() {
            return minimumAmount;
        }
    }
}