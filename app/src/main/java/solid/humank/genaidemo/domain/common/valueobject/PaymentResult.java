package solid.humank.genaidemo.domain.common.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/**
 * 支付結果值對象
 * 
 * 封裝支付操作的結果信息，包括支付ID、是否成功和消息。
 * 作為值對象，它是不可變的，所有屬性在創建後不能被修改。
 * 使用Java 16+ record特性實現，自動提供equals、hashCode和toString方法。
 */
@ValueObject
public record PaymentResult(
    String paymentId,
    boolean success,
    String message
) {
    /**
     * 創建成功的支付結果
     * 
     * @param paymentId 支付ID
     * @return 成功的支付結果
     */
    public static PaymentResult successful(String paymentId) {
        return new PaymentResult(paymentId, true, "支付成功");
    }

    /**
     * 創建失敗的支付結果
     * 
     * @param message 失敗消息
     * @return 失敗的支付結果
     */
    public static PaymentResult failed(String message) {
        return new PaymentResult(null, false, message);
    }

    /**
     * 驗證支付結果的有效性
     * 
     * @throws IllegalArgumentException 如果支付結果無效
     */
    public PaymentResult {
        if (success && (paymentId == null || paymentId.isBlank())) {
            throw new IllegalArgumentException("成功的支付結果必須包含支付ID");
        }
        if (!success && (message == null || message.isBlank())) {
            throw new IllegalArgumentException("失敗的支付結果必須包含錯誤信息");
        }
    }
}