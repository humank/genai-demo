package solid.humank.genaidemo.domain.payment.model.aggregate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.common.valueobject.PaymentStatus;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 支付聚合根單元測試
 */
class PaymentTest {

    @Test
    @DisplayName("建立支付時應設置正確的初始狀態")
    void shouldSetCorrectInitialStateWhenCreatingPayment() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        Money amount = Money.of(30000);

        // Act
        Payment payment = new Payment(orderId, amount);

        // Assert
        assertNotNull(payment.getId());
        assertEquals(orderId, payment.getOrderId());
        assertEquals(amount, payment.getAmount());
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        assertNull(payment.getTransactionId());
        assertNotNull(payment.getCreatedAt());
        assertEquals(payment.getCreatedAt(), payment.getUpdatedAt());
    }

    @Test
    @DisplayName("建立支付時若訂單ID為空應拋出異常")
    void shouldThrowExceptionWhenOrderIdIsNull() {
        // Arrange
        UUID orderId = null;
        Money amount = Money.of(30000);

        // Act & Assert
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new Payment(orderId, amount)
        );
        assertTrue(exception.getMessage().contains("Order ID cannot be null"));
    }

    @Test
    @DisplayName("建立支付時若金額為空應拋出異常")
    void shouldThrowExceptionWhenAmountIsNull() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        Money amount = null;

        // Act & Assert
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new Payment(orderId, amount)
        );
        assertTrue(exception.getMessage().contains("Amount cannot be null"));
    }

    @Test
    @DisplayName("完成支付時應更新狀態和交易ID")
    void shouldUpdateStatusAndTransactionIdWhenCompletingPayment() {
        // Arrange
        Payment payment = new Payment(UUID.randomUUID(), Money.of(30000));
        String transactionId = "txn_" + UUID.randomUUID().toString();

        // Act
        payment.complete(transactionId);

        // Assert
        assertEquals(PaymentStatus.COMPLETED, payment.getStatus());
        assertEquals(transactionId, payment.getTransactionId());
        assertNotEquals(payment.getCreatedAt(), payment.getUpdatedAt());
    }

    @Test
    @DisplayName("完成支付時若交易ID為空應拋出異常")
    void shouldThrowExceptionWhenTransactionIdIsNull() {
        // Arrange
        Payment payment = new Payment(UUID.randomUUID(), Money.of(30000));
        String transactionId = null;

        // Act & Assert
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> payment.complete(transactionId)
        );
        assertTrue(exception.getMessage().contains("Transaction ID cannot be null"));
    }

    @Test
    @DisplayName("非待處理狀態的支付不能完成")
    void shouldNotAllowCompletingPaymentWhenNotInPendingState() {
        // Arrange
        Payment payment = new Payment(UUID.randomUUID(), Money.of(30000));
        payment.complete("txn_123");

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> payment.complete("txn_456")
        );
        assertTrue(exception.getMessage().contains("Payment must be in PENDING state to complete"));
    }

    @Test
    @DisplayName("支付失敗時應更新狀態")
    void shouldUpdateStatusWhenPaymentFails() {
        // Arrange
        Payment payment = new Payment(UUID.randomUUID(), Money.of(30000));
        String failureReason = "Insufficient funds";

        // Act
        payment.fail(failureReason);

        // Assert
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        assertNotEquals(payment.getCreatedAt(), payment.getUpdatedAt());
    }

    @Test
    @DisplayName("非待處理狀態的支付不能標記為失敗")
    void shouldNotAllowFailingPaymentWhenNotInPendingState() {
        // Arrange
        Payment payment = new Payment(UUID.randomUUID(), Money.of(30000));
        payment.fail("Insufficient funds");

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> payment.fail("Another reason")
        );
        assertTrue(exception.getMessage().contains("Payment must be in PENDING state to fail"));
    }

    @Test
    @DisplayName("退款時應更新狀態")
    void shouldUpdateStatusWhenRefunding() {
        // Arrange
        Payment payment = new Payment(UUID.randomUUID(), Money.of(30000));
        payment.complete("txn_123");

        // Act
        payment.refund();

        // Assert
        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
        assertNotEquals(payment.getCreatedAt(), payment.getUpdatedAt());
    }

    @Test
    @DisplayName("非已完成狀態的支付不能退款")
    void shouldNotAllowRefundingWhenNotInCompletedState() {
        // Arrange
        Payment payment = new Payment(UUID.randomUUID(), Money.of(30000));
        // 支付仍處於 PENDING 狀態

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                payment::refund
        );
        assertTrue(exception.getMessage().contains("Payment must be in COMPLETED state to refund"));
    }
}