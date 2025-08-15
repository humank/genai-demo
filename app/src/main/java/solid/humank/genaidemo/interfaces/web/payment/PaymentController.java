package solid.humank.genaidemo.interfaces.web.payment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import solid.humank.genaidemo.application.common.dto.StandardErrorResponse;
import solid.humank.genaidemo.application.payment.dto.PaymentResponseDto;
import solid.humank.genaidemo.application.payment.dto.ProcessPaymentCommand;
import solid.humank.genaidemo.application.payment.port.incoming.PaymentManagementUseCase;
import solid.humank.genaidemo.interfaces.web.payment.dto.PaymentRequest;
import solid.humank.genaidemo.interfaces.web.payment.dto.PaymentResponse;

/** 支付控制器 處理支付相關的HTTP請求 */
@RestController
@RequestMapping("/api/payments")
@Tag(name = "支付管理", description = "處理支付相關操作，包括支付處理、查詢、退款和取消等功能")
@Validated
public class PaymentController {

    private final PaymentManagementUseCase paymentManagementUseCase;

    public PaymentController(PaymentManagementUseCase paymentManagementUseCase) {
        this.paymentManagementUseCase = paymentManagementUseCase;
    }

    /** 處理支付 */
    @PostMapping
    @Operation(summary = "處理支付", description = "處理新的支付請求，支援多種支付方式包括信用卡、轉帳等")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "201",
                        description = "支付處理成功",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = PaymentResponse.class))),
                @ApiResponse(
                        responseCode = "400",
                        description = "請求參數無效",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class))),
                @ApiResponse(
                        responseCode = "422",
                        description = "支付處理失敗（業務邏輯錯誤）",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class))),
                @ApiResponse(
                        responseCode = "500",
                        description = "系統內部錯誤",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class)))
            })
    public ResponseEntity<PaymentResponse> processPayment(
            @Parameter(description = "支付請求資料", required = true) @Valid @RequestBody
                    PaymentRequest request) {
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
    @Operation(summary = "獲取支付詳情", description = "根據支付ID獲取特定支付的詳細資訊")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "成功獲取支付詳情",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = PaymentResponse.class))),
                @ApiResponse(
                        responseCode = "400",
                        description = "支付ID格式無效",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class))),
                @ApiResponse(
                        responseCode = "404",
                        description = "支付不存在",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class))),
                @ApiResponse(
                        responseCode = "500",
                        description = "系統內部錯誤",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class)))
            })
    public ResponseEntity<PaymentResponse> getPayment(
            @Parameter(
                            description = "支付ID（UUID格式）",
                            required = true,
                            example = "123e4567-e89b-12d3-a456-426614174000")
                    @PathVariable
                    @Pattern(
                            regexp =
                                    "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
                            message = "支付ID必須是有效的UUID格式")
                    String paymentId) {
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
    @Operation(summary = "根據訂單ID獲取支付", description = "根據訂單ID獲取該訂單相關的支付資訊")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "成功獲取訂單支付詳情",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = PaymentResponse.class))),
                @ApiResponse(
                        responseCode = "400",
                        description = "訂單ID格式無效",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class))),
                @ApiResponse(
                        responseCode = "404",
                        description = "訂單支付不存在",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class))),
                @ApiResponse(
                        responseCode = "500",
                        description = "系統內部錯誤",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class)))
            })
    public ResponseEntity<PaymentResponse> getPaymentByOrderId(
            @Parameter(
                            description = "訂單ID（UUID格式）",
                            required = true,
                            example = "123e4567-e89b-12d3-a456-426614174000")
                    @PathVariable
                    @Pattern(
                            regexp =
                                    "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
                            message = "訂單ID必須是有效的UUID格式")
                    String orderId) {
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
    @Operation(summary = "獲取所有支付記錄", description = "獲取系統中所有支付記錄的列表，通常用於管理後台查詢")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "成功獲取支付列表",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        type = "array",
                                                        implementation = PaymentResponse.class))),
                @ApiResponse(
                        responseCode = "500",
                        description = "系統內部錯誤",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class)))
            })
    public ResponseEntity<List<PaymentResponse>> getAllPayments() {
        // 使用應用層DTO列表，避免直接依賴領域模型
        List<PaymentResponseDto> responseDtos = paymentManagementUseCase.getAllPaymentDtos();

        List<PaymentResponse> responses =
                responseDtos.stream().map(PaymentResponse::fromDto).collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /** 退款 */
    @PostMapping("/{paymentId}/refund")
    @Operation(summary = "處理退款", description = "對指定的支付進行退款處理，將款項退回給客戶")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "退款處理成功",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = PaymentResponse.class))),
                @ApiResponse(
                        responseCode = "400",
                        description = "支付ID格式無效",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class))),
                @ApiResponse(
                        responseCode = "404",
                        description = "支付不存在",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class))),
                @ApiResponse(
                        responseCode = "422",
                        description = "支付狀態不允許退款",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class))),
                @ApiResponse(
                        responseCode = "500",
                        description = "系統內部錯誤",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class)))
            })
    public ResponseEntity<PaymentResponse> refundPayment(
            @Parameter(
                            description = "支付ID（UUID格式）",
                            required = true,
                            example = "123e4567-e89b-12d3-a456-426614174000")
                    @PathVariable
                    @Pattern(
                            regexp =
                                    "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
                            message = "支付ID必須是有效的UUID格式")
                    String paymentId) {
        PaymentResponseDto responseDto =
                paymentManagementUseCase.refundPayment(UUID.fromString(paymentId));
        return ResponseEntity.ok(PaymentResponse.fromDto(responseDto));
    }

    /** 取消支付 */
    @PostMapping("/{paymentId}/cancel")
    @Operation(summary = "取消支付", description = "取消指定的支付，通常用於處理中的支付或預授權支付")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "支付取消成功",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = PaymentResponse.class))),
                @ApiResponse(
                        responseCode = "400",
                        description = "支付ID格式無效",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class))),
                @ApiResponse(
                        responseCode = "404",
                        description = "支付不存在",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class))),
                @ApiResponse(
                        responseCode = "422",
                        description = "支付狀態不允許取消",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class))),
                @ApiResponse(
                        responseCode = "500",
                        description = "系統內部錯誤",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class)))
            })
    public ResponseEntity<PaymentResponse> cancelPayment(
            @Parameter(
                            description = "支付ID（UUID格式）",
                            required = true,
                            example = "123e4567-e89b-12d3-a456-426614174000")
                    @PathVariable
                    @Pattern(
                            regexp =
                                    "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
                            message = "支付ID必須是有效的UUID格式")
                    String paymentId) {
        PaymentResponseDto responseDto =
                paymentManagementUseCase.cancelPayment(UUID.fromString(paymentId));
        return ResponseEntity.ok(PaymentResponse.fromDto(responseDto));
    }

    /** 處理異常 */
    @ExceptionHandler(IllegalArgumentException.class)
    @Operation(hidden = true)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    }
}
