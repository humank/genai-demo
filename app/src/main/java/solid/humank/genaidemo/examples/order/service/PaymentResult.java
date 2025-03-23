package solid.humank.genaidemo.examples.order.service;

import solid.humank.genaidemo.ddd.annotations.ValueObject;

/**
 * 支付結果值物件
 * 封裝支付操作的結果信息
 */
@ValueObject
public record PaymentResult(
    String paymentId,
    boolean success,
    String message
) {
    /**
     * 創建成功的支付結果
     */
    public static PaymentResult successful(String paymentId) {
        return new PaymentResult(paymentId, true, "支付成功");
    }

    /**
     * 創建失敗的支付結果
     */
    public static PaymentResult failed(String message) {
        return new PaymentResult(null, false, message);
    }

    /**
     * 驗證支付結果的有效性
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
