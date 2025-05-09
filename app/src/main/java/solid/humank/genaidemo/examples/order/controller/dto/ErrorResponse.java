package solid.humank.genaidemo.examples.order.controller.dto;

import java.util.List;
import java.util.Collections;

/**
 * 錯誤響應 DTO
 */
public class ErrorResponse {
    private final List<String> errors;

    public ErrorResponse(String error) {
        this.errors = Collections.singletonList(error);
    }

    public ErrorResponse(List<String> errors) {
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}