package solid.humank.genaidemo.exceptions;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/** 全局異常處理器 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Object> handleBusinessException(BusinessException ex) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(ex.getErrors()));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Object> handleValidationException(ValidationException ex) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(ex.getErrors()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(Exception ex) {
        // 在開發環境中顯示詳細錯誤信息
        String errorMessage = "系統發生未預期的錯誤";
        if (ex.getMessage() != null) {
            errorMessage += ": " + ex.getMessage();
        }

        // 記錄完整的異常堆棧
        ex.printStackTrace();

        return ResponseEntity.internalServerError()
                .body(new ApiErrorResponse(List.of(errorMessage)));
    }

    /** API錯誤響應 在異常處理器中定義，避免跨層依賴 */
    public static class ApiErrorResponse {
        private final List<String> errors;

        public ApiErrorResponse(String error) {
            this.errors = List.of(error);
        }

        public ApiErrorResponse(List<String> errors) {
            this.errors = errors;
        }

        public List<String> getErrors() {
            return errors;
        }
    }
}
