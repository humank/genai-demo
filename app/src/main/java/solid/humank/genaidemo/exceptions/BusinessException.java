package solid.humank.genaidemo.exceptions;

import java.util.List;

public class BusinessException extends RuntimeException {
    private final List<String> errors;

    public BusinessException(String message) {
        this(List.of(message));
    }

    public BusinessException(List<String> errors) {
        super(String.join(", ", errors));
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}
