package md.bnm.converter.core;

/**
 * Checked/runtime domain exception thrown when user input fails validation rules.
 */
public class ValidationException extends IllegalArgumentException {
    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
