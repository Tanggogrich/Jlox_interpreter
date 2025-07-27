package lox.exceptions;

// This is a special exception used internally by the interpreter
// to implement the 'break' statement. It does not carry an error message
// because it's not a user-facing error.
public class BreakException extends RuntimeException {
    // No constructor needed, as it's just a signal.
    // We don't need to fill in stack traces for performance,
    // as it's a common control flow mechanism.
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this; // Suppress stack trace for performance
    }
}
