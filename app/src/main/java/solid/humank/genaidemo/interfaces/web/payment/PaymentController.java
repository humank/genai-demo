package solid.humank.genaidemo.interfaces.web.payment;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import solid.humank.genaidemo.domain.order.model.valueobject.Money;
import solid.humank.genaidemo.application.payment.port.incoming.PaymentManagementUseCase;
import solid.humank.genaidemo.interfaces.web.payment.dto.PaymentRequest;
import solid.humank.genaidemo.interfaces.web.payment.dto.PaymentResponse;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 支付控制器
 * 處理支付相關的HTTP請求
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    
    private final PaymentManagementUseCase paymentManagementUseCase;
    
    public PaymentController(PaymentManagementUseCase paymentManagementUseCase) {
        this.paymentManagementUseCase = paymentManagementUseCase;
    }
    
    /**
     * 處理支付
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest request) {
        var payment = paymentManagementUseCase.processPayment(
                UUID.fromString(request.getOrderId()),
                Money.of(request.getAmount())
        );
        
        return new ResponseEntity<>(PaymentResponse.fromDomain(payment), HttpStatus.CREATED);
    }
    
    /**
     * 獲取支付
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable String paymentId) {
        var paymentOpt = paymentManagementUseCase.getPayment(UUID.fromString(paymentId));
        
        return paymentOpt
                .map(payment -> ResponseEntity.ok(PaymentResponse.fromDomain(payment)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 獲取訂單的支付
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> getPaymentByOrderId(@PathVariable String orderId) {
        var paymentOpt = paymentManagementUseCase.getPaymentByOrderId(UUID.fromString(orderId));
        
        return paymentOpt
                .map(payment -> ResponseEntity.ok(PaymentResponse.fromDomain(payment)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 獲取所有支付
     */
    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getAllPayments() {
        var payments = paymentManagementUseCase.getAllPayments()
                .stream()
                .map(PaymentResponse::fromDomain)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(payments);
    }
    
    /**
     * 退款
     */
    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<Void> refundPayment(@PathVariable String paymentId) {
        paymentManagementUseCase.refundPayment(UUID.fromString(paymentId));
        return ResponseEntity.ok().build();
    }
    
    /**
     * 處理異常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    }
}