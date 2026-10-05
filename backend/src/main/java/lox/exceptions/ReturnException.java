package lox.exceptions;

public class ReturnException extends RuntimeException {
    final Object value;

    public ReturnException(Object value) {
        super(null, null, false, false);
        this.value = value;
    }

    public Object getValue() {
        return value;
    }
}
