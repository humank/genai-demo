package solid.humank.genaidemo.application.payment.port.incoming;

import solid.humank.genaidemo.application.payment.dto.PaymentResponseDto;
import solid.humank.genaidemo.application.payment.dto.ProcessPaymentCommand;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.payment.model.aggregate.Payment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 支付管理用例
 * 定義支付管理的業務操作
 */
public interface PaymentManagementUseCase {
    
    /**
     * 處理支付（使用命令對象）
     */
    PaymentResponseDto processPayment(ProcessPaymentCommand command);
    
    /**
     * 處理支付（簡化版）
     */
    Payment processPayment(UUID orderId, Money amount);
    
    /**
     * 獲取支付
     */
    Optional<Payment> getPayment(UUID paymentId);
    
    /**
     * 獲取支付DTO
     */
    PaymentResponseDto getPaymentDto(UUID paymentId);
    
    /**
     * 獲取訂單的支付
     */
    Optional<Payment> getPaymentByOrderId(UUID orderId);
    
    /**
     * 獲取訂單的支付DTO
     */
    PaymentResponseDto getPaymentDtoByOrderId(UUID orderId);
    
    /**
     * 獲取所有支付
     */
    List<Payment> getAllPayments();
    
    /**
     * 獲取所有支付DTO
     */
    List<PaymentResponseDto> getAllPaymentDtos();
    
    /**
     * 退款
     */
    PaymentResponseDto refundPayment(UUID paymentId);
    
    /**
     * 取消支付
     */
    PaymentResponseDto cancelPayment(UUID paymentId);
}