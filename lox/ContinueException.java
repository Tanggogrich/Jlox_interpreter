package lox;

// This is a special exception used internally by the interpreter
// to implement the 'continue' statement. It does not carry an error message.
public class ContinueException extends RuntimeException {
    // Suppress stack trace for performance
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
