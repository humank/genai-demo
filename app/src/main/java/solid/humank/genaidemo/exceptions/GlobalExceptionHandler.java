package solid.humank.genaidemo.exceptions;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import solid.humank.genaidemo.examples.order.controller.dto.ErrorResponse;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Object> handleBusinessException(BusinessException ex) {
        return ResponseEntity.badRequest()
            .body(new ErrorResponse(ex.getErrors()));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Object> handleValidationException(ValidationException ex) {
        return ResponseEntity.badRequest()
            .body(new ErrorResponse(ex.getErrors()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(Exception ex) {
        return ResponseEntity.internalServerError()
            .body(new ErrorResponse(List.of("系統發生未預期的錯誤")));
    }
}
