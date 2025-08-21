package solid.humank.genaidemo.application.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 標準錯誤代碼列舉 定義系統中所有可能的錯誤類型 */
@Schema(description = "標準錯誤代碼")
public enum ErrorCode {
    @Schema(description = "請求參數驗證失敗")
    VALIDATION_ERROR("VALIDATION_ERROR", "請求參數驗證失敗"),

    @Schema(description = "請求的資源不存在")
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "請求的資源不存在"),

    @Schema(description = "違反業務規則")
    BUSINESS_RULE_VIOLATION("BUSINESS_RULE_VIOLATION", "違反業務規則"),

    @Schema(description = "庫存不足")
    INSUFFICIENT_INVENTORY("INSUFFICIENT_INVENTORY", "庫存不足"),

    @Schema(description = "訂單狀態不允許此操作")
    INVALID_ORDER_STATUS("INVALID_ORDER_STATUS", "訂單狀態不允許此操作"),

    @Schema(description = "支付處理失敗")
    PAYMENT_PROCESSING_ERROR("PAYMENT_PROCESSING_ERROR", "支付處理失敗"),

    @Schema(description = "權限不足")
    INSUFFICIENT_PERMISSIONS("INSUFFICIENT_PERMISSIONS", "權限不足"),

    @Schema(description = "請求頻率過高")
    RATE_LIMIT_EXCEEDED("RATE_LIMIT_EXCEEDED", "請求頻率過高"),

    @Schema(description = "系統內部錯誤")
    SYSTEM_ERROR("SYSTEM_ERROR", "系統內部錯誤"),

    @Schema(description = "外部服務不可用")
    EXTERNAL_SERVICE_UNAVAILABLE("EXTERNAL_SERVICE_UNAVAILABLE", "外部服務不可用");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Schema(description = "錯誤代碼", example = "VALIDATION_ERROR")
    public String getCode() {
        return code;
    }

    @Schema(description = "錯誤訊息", example = "請求參數驗證失敗")
    public String getMessage() {
        return message;
    }
}
