package solid.humank.genaidemo.interfaces.web.payment;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import solid.humank.genaidemo.application.payment.dto.PaymentResponseDto;
import solid.humank.genaidemo.application.payment.dto.ProcessPaymentCommand;
import solid.humank.genaidemo.application.payment.port.incoming.PaymentManagementUseCase;
import solid.humank.genaidemo.interfaces.web.payment.dto.PaymentRequest;
import solid.humank.genaidemo.interfaces.web.payment.dto.PaymentResponse;

/** 支付控制器 處理支付相關的HTTP請求 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentManagementUseCase paymentManagementUseCase;

    public PaymentController(PaymentManagementUseCase paymentManagementUseCase) {
        this.paymentManagementUseCase = paymentManagementUseCase;
    }

    /** 處理支付 */
    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest request) {
        // 創建處理支付命令
        ProcessPaymentCommand command;

        if (request.getPaymentDetails() != null) {
            // 帶支付詳情
            command =
                    ProcessPaymentCommand.of(
                            request.getOrderId(),
                            request.getAmount(),
                            request.getCurrency() != null ? request.getCurrency() : "TWD",
                            request.getPaymentMethod() != null
                                    ? request.getPaymentMethod()
                                    : "CREDIT_CARD",
                            request.getPaymentDetails().toString());
        } else {
            // 不帶支付詳情
            command =
                    ProcessPaymentCommand.of(
                            request.getOrderId(),
                            request.getAmount(),
                            request.getCurrency() != null ? request.getCurrency() : "TWD",
                            request.getPaymentMethod() != null
                                    ? request.getPaymentMethod()
                                    : "CREDIT_CARD");
        }

        // 處理支付
        PaymentResponseDto responseDto = paymentManagementUseCase.processPayment(command);

        // 轉換為介面層 DTO
        PaymentResponse response = PaymentResponse.fromDto(responseDto);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /** 獲取支付 */
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable String paymentId) {
        var paymentOpt = paymentManagementUseCase.getPayment(UUID.fromString(paymentId));

        if (paymentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // 使用應用層DTO，避免直接依賴領域模型
        PaymentResponseDto responseDto =
                paymentManagementUseCase.getPaymentDto(UUID.fromString(paymentId));
        return ResponseEntity.ok(PaymentResponse.fromDto(responseDto));
    }

    /** 獲取訂單的支付 */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> getPaymentByOrderId(@PathVariable String orderId) {
        var paymentOpt = paymentManagementUseCase.getPaymentByOrderId(UUID.fromString(orderId));

        if (paymentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // 使用應用層DTO，避免直接依賴領域模型
        PaymentResponseDto responseDto =
                paymentManagementUseCase.getPaymentDtoByOrderId(UUID.fromString(orderId));
        return ResponseEntity.ok(PaymentResponse.fromDto(responseDto));
    }

    /** 獲取所有支付 */
    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getAllPayments() {
        // 使用應用層DTO列表，避免直接依賴領域模型
        List<PaymentResponseDto> responseDtos = paymentManagementUseCase.getAllPaymentDtos();

        List<PaymentResponse> responses =
                responseDtos.stream().map(PaymentResponse::fromDto).collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /** 退款 */
    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<PaymentResponse> refundPayment(@PathVariable String paymentId) {
        PaymentResponseDto responseDto =
                paymentManagementUseCase.refundPayment(UUID.fromString(paymentId));
        return ResponseEntity.ok(PaymentResponse.fromDto(responseDto));
    }

    /** 取消支付 */
    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<PaymentResponse> cancelPayment(@PathVariable String paymentId) {
        PaymentResponseDto responseDto =
                paymentManagementUseCase.cancelPayment(UUID.fromString(paymentId));
        return ResponseEntity.ok(PaymentResponse.fromDto(responseDto));
    }

    /** 處理異常 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    }
}
