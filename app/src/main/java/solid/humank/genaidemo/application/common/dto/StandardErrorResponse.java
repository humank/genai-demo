package solid.humank.genaidemo.application.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/** 標準錯誤回應 提供統一的錯誤回應格式，包含錯誤代碼、訊息、時間戳記和詳細資訊 */
@Schema(description = "標準錯誤回應")
public class StandardErrorResponse {

    @Schema(description = "錯誤代碼", example = "VALIDATION_ERROR", required = true)
    private String code;

    @Schema(description = "錯誤訊息", example = "請求參數驗證失敗", required = true)
    private String message;

    @Schema(description = "錯誤發生時間", example = "2024-01-15T10:30:00", required = true)
    private LocalDateTime timestamp;

    @Schema(description = "請求路徑", example = "/api/orders", required = true)
    private String path;

    @Schema(description = "詳細錯誤資訊", nullable = true)
    private List<FieldError> details;

    @Schema(description = "追蹤ID，用於問題排查", example = "abc123-def456-ghi789", nullable = true)
    private String traceId;

    // 預設建構子
    public StandardErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }

    // 建構子 - 基本錯誤資訊
    public StandardErrorResponse(ErrorCode errorCode, String path) {
        this();
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
        this.path = path;
    }

    // 建構子 - 自訂錯誤訊息
    public StandardErrorResponse(ErrorCode errorCode, String customMessage, String path) {
        this();
        this.code = errorCode.getCode();
        this.message = customMessage;
        this.path = path;
    }

    // 建構子 - 包含詳細錯誤資訊
    public StandardErrorResponse(ErrorCode errorCode, String path, List<FieldError> details) {
        this(errorCode, path);
        this.details = details;
    }

    // 建構子 - 完整資訊
    public StandardErrorResponse(
            ErrorCode errorCode,
            String customMessage,
            String path,
            List<FieldError> details,
            String traceId) {
        this();
        this.code = errorCode.getCode();
        this.message = customMessage;
        this.path = path;
        this.details = details;
        this.traceId = traceId;
    }

    // Getters and Setters
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<FieldError> getDetails() {
        return details;
    }

    public void setDetails(List<FieldError> details) {
        this.details = details;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    /** 欄位錯誤詳情 用於描述特定欄位的驗證錯誤 */
    @Schema(description = "欄位錯誤詳情")
    public static class FieldError {

        @Schema(description = "欄位名稱", example = "customerId", required = true)
        private String field;

        @Schema(description = "錯誤訊息", example = "客戶ID不能為空", required = true)
        private String message;

        @Schema(description = "拒絕的值", example = "null", nullable = true)
        private Object rejectedValue;

        @Schema(description = "錯誤代碼", example = "NotBlank", nullable = true)
        private String code;

        // 預設建構子
        public FieldError() {}

        // 建構子
        public FieldError(String field, String message) {
            this.field = field;
            this.message = message;
        }

        // 建構子 - 包含拒絕值
        public FieldError(String field, String message, Object rejectedValue) {
            this.field = field;
            this.message = message;
            this.rejectedValue = rejectedValue;
        }

        // 建構子 - 完整資訊
        public FieldError(String field, String message, Object rejectedValue, String code) {
            this.field = field;
            this.message = message;
            this.rejectedValue = rejectedValue;
            this.code = code;
        }

        // Getters and Setters
        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Object getRejectedValue() {
            return rejectedValue;
        }

        public void setRejectedValue(Object rejectedValue) {
            this.rejectedValue = rejectedValue;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }
}
