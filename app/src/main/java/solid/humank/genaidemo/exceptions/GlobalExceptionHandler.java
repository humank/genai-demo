package solid.humank.genaidemo.exceptions;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import solid.humank.genaidemo.application.common.dto.ErrorCode;
import solid.humank.genaidemo.application.common.dto.StandardErrorResponse;

/** 全域例外處理器 統一處理各種例外情況，回傳標準化的錯誤回應 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 處理業務邏輯例外 */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    @ApiResponse(
            responseCode = "422",
            description = "業務規則違反",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardErrorResponse.class)))
    @Hidden
    public StandardErrorResponse handleBusinessException(
            BusinessException ex, HttpServletRequest request) {

        String traceId = generateTraceId();
        logger.warn(
                "Business exception occurred - TraceId: {}, Path: {}, Message: {}",
                traceId,
                request.getRequestURI(),
                ex.getMessage());

        // 如果有多個錯誤訊息，轉換為 FieldError
        List<StandardErrorResponse.FieldError> details = null;
        if (ex.getErrors().size() > 1) {
            details =
                    ex.getErrors().stream()
                            .map(error -> new StandardErrorResponse.FieldError("business", error))
                            .collect(Collectors.toList());
        }

        return new StandardErrorResponse(
                ErrorCode.BUSINESS_RULE_VIOLATION,
                ex.getMessage(),
                request.getRequestURI(),
                details,
                traceId);
    }

    /** 處理驗證例外 */
    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ApiResponse(
            responseCode = "400",
            description = "請求參數驗證失敗",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardErrorResponse.class)))
    @Hidden
    public StandardErrorResponse handleValidationException(
            ValidationException ex, HttpServletRequest request) {

        String traceId = generateTraceId();
        logger.warn(
                "Validation exception occurred - TraceId: {}, Path: {}, Errors: {}",
                traceId,
                request.getRequestURI(),
                ex.getErrors());

        List<StandardErrorResponse.FieldError> details =
                ex.getErrors().stream()
                        .map(error -> new StandardErrorResponse.FieldError("validation", error))
                        .collect(Collectors.toList());

        return new StandardErrorResponse(
                ErrorCode.VALIDATION_ERROR, request.getRequestURI(), details);
    }

    /** 處理 Bean Validation 例外 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ApiResponse(
            responseCode = "400",
            description = "請求參數驗證失敗",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardErrorResponse.class)))
    @Hidden
    public StandardErrorResponse handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        String traceId = generateTraceId();
        logger.warn(
                "Method argument validation failed - TraceId: {}, Path: {}",
                traceId,
                request.getRequestURI());

        List<StandardErrorResponse.FieldError> details =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(
                                fieldError ->
                                        new StandardErrorResponse.FieldError(
                                                fieldError.getField(),
                                                fieldError.getDefaultMessage(),
                                                fieldError.getRejectedValue(),
                                                fieldError.getCode()))
                        .collect(Collectors.toList());

        return new StandardErrorResponse(
                ErrorCode.VALIDATION_ERROR, "請求參數驗證失敗", request.getRequestURI(), details, traceId);
    }

    /** 處理約束違反例外 */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ApiResponse(
            responseCode = "400",
            description = "約束驗證失敗",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardErrorResponse.class)))
    @Hidden
    public StandardErrorResponse handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {

        String traceId = generateTraceId();
        logger.warn(
                "Constraint violation occurred - TraceId: {}, Path: {}",
                traceId,
                request.getRequestURI());

        List<StandardErrorResponse.FieldError> details =
                ex.getConstraintViolations().stream()
                        .map(
                                violation ->
                                        new StandardErrorResponse.FieldError(
                                                getFieldName(violation),
                                                violation.getMessage(),
                                                violation.getInvalidValue()))
                        .collect(Collectors.toList());

        return new StandardErrorResponse(
                ErrorCode.VALIDATION_ERROR, "約束驗證失敗", request.getRequestURI(), details, traceId);
    }

    /** 處理參數類型不匹配例外 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ApiResponse(
            responseCode = "400",
            description = "參數類型不匹配",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardErrorResponse.class)))
    @Hidden
    public StandardErrorResponse handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        String traceId = generateTraceId();
        logger.warn(
                "Method argument type mismatch - TraceId: {}, Path: {}, Parameter: {}",
                traceId,
                request.getRequestURI(),
                ex.getName());

        StandardErrorResponse.FieldError detail =
                new StandardErrorResponse.FieldError(
                        ex.getName(),
                        String.format(
                                "參數 '%s' 的值 '%s' 無法轉換為 %s 類型",
                                ex.getName(), ex.getValue(), ex.getRequiredType().getSimpleName()),
                        ex.getValue());

        return new StandardErrorResponse(
                ErrorCode.VALIDATION_ERROR,
                "參數類型不匹配",
                request.getRequestURI(),
                List.of(detail),
                traceId);
    }

    /** 處理資源不存在例外 */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ApiResponse(
            responseCode = "404",
            description = "請求的資源不存在",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardErrorResponse.class)))
    @Hidden
    public StandardErrorResponse handleNoHandlerFoundException(
            NoHandlerFoundException ex, HttpServletRequest request) {

        String traceId = generateTraceId();
        logger.warn(
                "No handler found - TraceId: {}, Path: {}, Method: {}",
                traceId,
                request.getRequestURI(),
                ex.getHttpMethod());

        return new StandardErrorResponse(
                ErrorCode.RESOURCE_NOT_FOUND,
                String.format("找不到 %s %s 的處理器", ex.getHttpMethod(), ex.getRequestURL()),
                request.getRequestURI(),
                null,
                traceId);
    }

    /** 處理一般例外 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ApiResponse(
            responseCode = "500",
            description = "系統內部錯誤",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardErrorResponse.class)))
    @Hidden
    public StandardErrorResponse handleGenericException(Exception ex, HttpServletRequest request) {

        String traceId = generateTraceId();
        logger.error(
                "Unexpected exception occurred - TraceId: {}, Path: {}",
                traceId,
                request.getRequestURI(),
                ex);

        // 在生產環境中不暴露詳細的錯誤資訊
        String message = "系統發生未預期的錯誤，請稍後再試";

        return new StandardErrorResponse(
                ErrorCode.SYSTEM_ERROR, message, request.getRequestURI(), null, traceId);
    }

    /** 生成追蹤ID */
    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    /** 從約束違反中提取欄位名稱 */
    private String getFieldName(ConstraintViolation<?> violation) {
        String propertyPath = violation.getPropertyPath().toString();
        return propertyPath.isEmpty() ? "unknown" : propertyPath;
    }
}
