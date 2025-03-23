package solid.humank.genaidemo.exceptions;

import java.util.List;

public class ValidationException extends RuntimeException {
    private final List<String> errors;

    public ValidationException(String message) {
        this(List.of(message));
    }

    public ValidationException(List<String> errors) {
        super(String.join(", ", errors));
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}
