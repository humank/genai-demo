package solid.humank.genaidemo.domain.payment.service;

import solid.humank.genaidemo.domain.common.annotations.DomainService;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.common.valueobject.PaymentId;
import solid.humank.genaidemo.domain.payment.model.aggregate.Payment;
import solid.humank.genaidemo.domain.payment.model.aggregate.PaymentMethod;
import solid.humank.genaidemo.domain.payment.repository.PaymentMethodRepository;
import solid.humank.genaidemo.domain.payment.repository.PaymentRepository;

/**
 * 支付領域服務
 * 處理支付相關的複雜業務邏輯和跨聚合根操作
 */
@DomainService(name = "PaymentDomainService", description = "支付領域服務，處理支付流程的複雜業務邏輯", boundedContext = "Payment")
public class PaymentDomainService {

    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    public PaymentDomainService(PaymentRepository paymentRepository,
            PaymentMethodRepository paymentMethodRepository) {
        this.paymentRepository = paymentRepository;
        this.paymentMethodRepository = paymentMethodRepository;
    }

    /**
     * 驗證支付方式是否有效
     * 
     * @param customerId      客戶ID
     * @param paymentMethodId 支付方式ID
     * @return 是否有效
     */
    public boolean isPaymentMethodValid(String customerId, String paymentMethodId) {
        PaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId).orElse(null);
        return paymentMethod != null &&
                paymentMethod.getCustomerId().getValue().equals(customerId) &&
                paymentMethod.isActive();
    }

    /**
     * 檢查支付金額是否在限制範圍內
     * 
     * @param paymentMethodId 支付方式ID
     * @param amount          支付金額
     * @return 是否在限制範圍內
     */
    public boolean isAmountWithinLimits(String paymentMethodId, Money amount) {
        PaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId).orElse(null);
        if (paymentMethod == null) {
            return false;
        }
        // 簡化實現 - 假設所有金額都在限制範圍內
        return amount.getAmount().compareTo(java.math.BigDecimal.ZERO) > 0;
    }

    /**
     * 處理支付重試邏輯
     * 
     * @param paymentId 支付ID
     * @return 是否可以重試
     */
    public boolean canRetryPayment(String paymentId) {
        Payment payment = paymentRepository.findById(PaymentId.fromString(paymentId)).orElse(null);
        if (payment == null) {
            return false;
        }
        return payment.canRetry();
    }

    /**
     * 執行支付重試
     * 
     * @param paymentId 支付ID
     * @return 重試是否成功
     */
    public boolean retryPayment(String paymentId) {
        Payment payment = paymentRepository.findById(PaymentId.fromString(paymentId)).orElse(null);
        if (payment == null || !payment.canRetry()) {
            return false;
        }

        payment.retry();
        paymentRepository.save(payment);
        return true;
    }

    /**
     * 計算支付手續費
     * 
     * @param paymentMethodId 支付方式ID
     * @param amount          支付金額
     * @return 手續費金額
     */
    public Money calculateProcessingFee(String paymentMethodId, Money amount) {
        PaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId).orElse(null);
        if (paymentMethod == null) {
            return Money.ZERO;
        }
        // 簡化實現 - 固定手續費率 2%
        return amount.multiply(0.02);
    }

    /**
     * 驗證退款是否允許
     * 
     * @param paymentId    支付ID
     * @param refundAmount 退款金額
     * @return 是否允許退款
     */
    public boolean canRefund(String paymentId, Money refundAmount) {
        Payment payment = paymentRepository.findById(PaymentId.fromString(paymentId)).orElse(null);
        if (payment == null) {
            return false;
        }
        // 簡化實現 - 檢查退款金額不超過原支付金額
        return refundAmount.getAmount().compareTo(payment.getAmount().getAmount()) <= 0;
    }

    /**
     * 處理部分退款
     * 
     * @param paymentId    支付ID
     * @param refundAmount 退款金額
     * @param reason       退款原因
     * @return 是否退款成功
     */
    public boolean processPartialRefund(String paymentId, Money refundAmount, String reason) {
        Payment payment = paymentRepository.findById(PaymentId.fromString(paymentId)).orElse(null);
        if (payment == null || refundAmount.getAmount().compareTo(payment.getAmount().getAmount()) > 0) {
            return false;
        }

        // 簡化實現 - 直接處理退款
        payment.refund();
        paymentRepository.save(payment);
        return true;
    }

    /**
     * 檢查支付是否需要額外驗證
     * 
     * @param customerId 客戶ID
     * @param amount     支付金額
     * @return 是否需要額外驗證
     */
    public boolean requiresAdditionalVerification(String customerId, Money amount) {
        // 大額支付需要額外驗證
        if (amount.isGreaterThan(Money.of(10000))) {
            return true;
        }

        // 檢查客戶的支付歷史是否有異常
        // 這裡可以加入更複雜的風險評估邏輯
        return false;
    }
}