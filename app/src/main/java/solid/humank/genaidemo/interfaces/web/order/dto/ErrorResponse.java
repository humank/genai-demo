package solid.humank.genaidemo.interfaces.web.order.dto;

import java.util.List;

/** 錯誤響應 DTO */
public class ErrorResponse {
    private final List<String> errors;

    public ErrorResponse(String error) {
        this.errors = List.of(error);
    }

    public ErrorResponse(List<String> errors) {
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}
