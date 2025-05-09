package solid.humank.genaidemo.interfaces.web.payment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import solid.humank.genaidemo.application.payment.port.incoming.PaymentManagementUseCase;
import solid.humank.genaidemo.domain.order.model.valueobject.Money;
import solid.humank.genaidemo.domain.payment.model.aggregate.Payment;
import solid.humank.genaidemo.domain.payment.model.valueobject.PaymentStatus;
import solid.humank.genaidemo.interfaces.web.payment.dto.PaymentRequest;
import solid.humank.genaidemo.interfaces.web.payment.dto.PaymentResponse;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 支付控制器單元測試
 * 使用 Mockito 進行單元測試，不啟動 Spring 容器
 */
@ExtendWith(MockitoExtension.class)
class PaymentControllerMockTest {

    @Mock
    private PaymentManagementUseCase paymentManagementUseCase;

    @InjectMocks
    private PaymentController paymentController;

    @Test
    @DisplayName("處理支付應返回201狀態碼和支付詳情")
    void processPaymentShouldReturn201AndPaymentDetails() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("30000");
        
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(orderId.toString());
        request.setAmount(amount);
        request.setCurrency("TWD");
        
        Payment mockPayment = new Payment(orderId, Money.of(amount));
        mockPayment.complete("txn_" + UUID.randomUUID().toString());
        
        when(paymentManagementUseCase.processPayment(eq(orderId), any(Money.class))).thenReturn(mockPayment);

        // Act
        ResponseEntity<PaymentResponse> response = paymentController.processPayment(request);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(orderId.toString(), response.getBody().getOrderId());
        assertEquals(amount.doubleValue(), response.getBody().getAmount().doubleValue());
        assertEquals(PaymentStatus.COMPLETED.toString(), response.getBody().getStatus());
    }

    @Test
    @DisplayName("獲取支付應返回200狀態碼和支付詳情")
    void getPaymentShouldReturn200AndPaymentDetails() {
        // Arrange
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        
        Payment mockPayment = new Payment(orderId, Money.of(30000));
        mockPayment.complete("txn_" + UUID.randomUUID().toString());
        
        when(paymentManagementUseCase.getPayment(paymentId)).thenReturn(Optional.of(mockPayment));

        // Act
        ResponseEntity<PaymentResponse> response = paymentController.getPayment(paymentId.toString());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(orderId.toString(), response.getBody().getOrderId());
        assertEquals(PaymentStatus.COMPLETED.toString(), response.getBody().getStatus());
    }

    @Test
    @DisplayName("獲取不存在的支付應返回404狀態碼")
    void getNonExistentPaymentShouldReturn404() {
        // Arrange
        UUID paymentId = UUID.randomUUID();
        
        when(paymentManagementUseCase.getPayment(paymentId)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<PaymentResponse> response = paymentController.getPayment(paymentId.toString());

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @DisplayName("獲取訂單的支付應返回200狀態碼和支付詳情")
    void getPaymentByOrderIdShouldReturn200AndPaymentDetails() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        
        Payment mockPayment = new Payment(orderId, Money.of(30000));
        mockPayment.complete("txn_" + UUID.randomUUID().toString());
        
        when(paymentManagementUseCase.getPaymentByOrderId(orderId)).thenReturn(Optional.of(mockPayment));

        // Act
        ResponseEntity<PaymentResponse> response = paymentController.getPaymentByOrderId(orderId.toString());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(orderId.toString(), response.getBody().getOrderId());
        assertEquals(PaymentStatus.COMPLETED.toString(), response.getBody().getStatus());
    }

    @Test
    @DisplayName("獲取所有支付應返回200狀態碼和支付列表")
    void getAllPaymentsShouldReturn200AndPaymentList() {
        // Arrange
        Payment mockPayment1 = new Payment(UUID.randomUUID(), Money.of(30000));
        mockPayment1.complete("txn_1");
        
        Payment mockPayment2 = new Payment(UUID.randomUUID(), Money.of(15000));
        mockPayment2.complete("txn_2");
        
        when(paymentManagementUseCase.getAllPayments()).thenReturn(Arrays.asList(mockPayment1, mockPayment2));

        // Act
        ResponseEntity<List<PaymentResponse>> response = paymentController.getAllPayments();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals(PaymentStatus.COMPLETED.toString(), response.getBody().get(0).getStatus());
        assertEquals(PaymentStatus.COMPLETED.toString(), response.getBody().get(1).getStatus());
    }

    @Test
    @DisplayName("退款應返回200狀態碼")
    void refundPaymentShouldReturn200() {
        // Arrange
        UUID paymentId = UUID.randomUUID();
        
        doNothing().when(paymentManagementUseCase).refundPayment(paymentId);

        // Act
        ResponseEntity<Void> response = paymentController.refundPayment(paymentId.toString());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(paymentManagementUseCase, times(1)).refundPayment(paymentId);
    }

    @Test
    @DisplayName("處理無效支付請求應返回400狀態碼")
    void processInvalidPaymentShouldReturn400() {
        // Arrange
        String errorMessage = "Amount cannot be negative";
        IllegalArgumentException exception = new IllegalArgumentException(errorMessage);

        // Act
        ResponseEntity<String> response = paymentController.handleIllegalArgumentException(exception);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(errorMessage, response.getBody());
    }
}