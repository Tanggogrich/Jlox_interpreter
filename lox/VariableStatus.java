package lox;

// Helper class to track the status of a variable within a scope.
public class VariableStatus {
    final Token declarationToken; // The token where the variable was declared.
    boolean isDefined;            // True if the variable has been initialized/defined.
    boolean isUsed;               // True if the variable has been read from.

    VariableStatus(Token declarationToken, boolean isDefined) {
        this.declarationToken = declarationToken;
        this.isDefined = isDefined;
        this.isUsed = false; // Initially, no variable is used.
    }
}
