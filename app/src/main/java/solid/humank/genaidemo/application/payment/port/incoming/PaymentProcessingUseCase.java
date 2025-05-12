package solid.humank.genaidemo.application.payment.port.incoming;

import solid.humank.genaidemo.application.payment.dto.PaymentRequestDto;
import solid.humank.genaidemo.application.payment.dto.PaymentResponseDto;

/**
 * 支付處理用例
 * 定義支付處理的業務操作
 */
public interface PaymentProcessingUseCase {
    
    /**
     * 處理支付
     * 
     * @param requestDto 支付請求DTO
     * @return 支付響應DTO
     */
    PaymentResponseDto processPayment(PaymentRequestDto requestDto);
}