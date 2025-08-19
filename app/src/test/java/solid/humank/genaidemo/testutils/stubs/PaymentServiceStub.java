package solid.humank.genaidemo.testutils.stubs;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import solid.humank.genaidemo.testutils.isolation.TestResource;

/**
 * Payment Service Stub
 */
public class PaymentServiceStub implements TestResource {

    private final List<PaymentRecord> processedPayments = new CopyOnWriteArrayList<>();
    private boolean shouldFailPayment = false;
    private String failureReason = "Payment failed";
    private boolean isCleanedUp = false;

    public PaymentResult processPayment(String paymentMethod, BigDecimal amount, String orderId) {
        if (isCleanedUp) {
            throw new IllegalStateException("Service has been cleaned up");
        }

        PaymentRecord record = new PaymentRecord(paymentMethod, amount, orderId, System.currentTimeMillis());
        processedPayments.add(record);

        if (shouldFailPayment) {
            return new PaymentResult(false, failureReason, null);
        }

        String transactionId = "TXN-" + System.currentTimeMillis();
        return new PaymentResult(true, "Payment successful", transactionId);
    }

    public void setPaymentFailure(boolean shouldFail, String reason) {
        this.shouldFailPayment = shouldFail;
        this.failureReason = reason;
    }

    public List<PaymentRecord> getProcessedPayments() {
        return List.copyOf(processedPayments);
    }

    public int getPaymentCount() {
        return isCleanedUp ? 0 : processedPayments.size();
    }

    public boolean hasPaymentFor(String orderId) {
        return !isCleanedUp && processedPayments.stream()
                .anyMatch(record -> record.orderId().equals(orderId));
    }

    /**
     * 處理支付（簡化版本，用於測試）
     */
    public PaymentResult processPayment(String paymentId, double amount) {
        if (isCleanedUp) {
            throw new IllegalStateException("Service has been cleaned up");
        }

        BigDecimal bigDecimalAmount = BigDecimal.valueOf(amount);
        PaymentRecord record = new PaymentRecord("CREDIT_CARD", bigDecimalAmount, paymentId,
                System.currentTimeMillis());
        processedPayments.add(record);

        if (shouldFailPayment) {
            return new PaymentResult(false, failureReason, null);
        }

        String transactionId = "TXN-" + System.currentTimeMillis();
        return new PaymentResult(true, "Payment successful", transactionId);
    }

    /**
     * 檢查是否有特定支付ID的支付記錄
     */
    public boolean hasPayment(String paymentId) {
        return !isCleanedUp && processedPayments.stream()
                .anyMatch(record -> record.orderId().equals(paymentId));
    }

    public void clear() {
        if (!isCleanedUp) {
            processedPayments.clear();
            shouldFailPayment = false;
            failureReason = "Payment failed";
        }
    }

    @Override
    public void cleanup() throws Exception {
        if (!isCleanedUp) {
            clear();
            isCleanedUp = true;
        }
    }

    @Override
    public String getResourceName() {
        return "PaymentServiceStub";
    }

    @Override
    public boolean isCleanedUp() {
        return isCleanedUp;
    }

    public record PaymentRecord(String paymentMethod, BigDecimal amount, String orderId, long timestamp) {
    }

    public record PaymentResult(boolean success, String message, String transactionId) {
    }
}