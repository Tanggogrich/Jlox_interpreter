package lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
    private final Interpreter interpreter;
    private final Stack<Map<String, VariableStatus>> scopes = new Stack<>();
    private FunctionType currentFunction = FunctionType.NONE;

    public Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    private enum FunctionType {
        NONE,
        FUNCTION
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        beginScope();
        resolve(stmt.statements);
        endScope();
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        resolve(stmt.condition);
        resolve(stmt.thenBranch);
        if (stmt.elseBranch != null) {
            resolve(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        if (currentFunction == FunctionType.NONE) {
            Lox.error(stmt.keyword, "Can't return from top-level code.");
        }

        if (stmt.value != null) {
            resolve(stmt.value);
        }
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        resolve(stmt.condition);
        resolve(stmt.body);
        return null;
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        if (currentFunction == FunctionType.NONE) {
            Lox.error(stmt.keyword, "Can't break from top-level code.");
        }
        return null;
    }

    @Override
    public Void visitContinueStmt(Stmt.Continue stmt) {
        if (currentFunction == FunctionType.NONE) {
            Lox.error(stmt.keyword, "Can't continue from top-level code.");
        }
        return null;
    }

    @Override
    public Void visitBinaryExpr(Expr.Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitCallExpr(Expr.Call expr) {
        resolve(expr.callee);
        for (Expr argument: expr.arguments) {
            resolve(argument);
        }
        return null;
    }

    @Override
    public Void visitBinaryRPNExpr(Expr.BinaryRPN expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitTernaryExpr(Expr.Ternary expr) {
        resolve(expr.condition);
        resolve(expr.thenBranch);
        if (expr.elseBranch != null) {
            resolve(expr.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitGroupingExpr(Expr.Grouping expr) {
        resolve(expr.expression);
        return null;
    }

    @Override
    public Void visitLiteralExpr(Expr.Literal expr) {
        return null;
    }

    @Override
    public Void visitLogicalExpr(Expr.Logical expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitUnaryExpr(Expr.Unary expr) {
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        declare(stmt.name);
        define(stmt.name);

        resolveCallable(stmt, FunctionType.FUNCTION);
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        declare(stmt.name);
        if (stmt.initializer != null) {
            resolve(stmt.initializer);
        }
        define(stmt.name);
        return null;
    }

    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        if (!scopes.isEmpty()) {
            Map<String, VariableStatus> scope = scopes.peek();
            if (scope.containsKey(expr.name.lexeme())) {
                VariableStatus status = scope.get(expr.name.lexeme());
                if (!status.isDefined) { // Check if defined
                    Lox.error(expr.name, "Can't read local variable in its own initializer.");
                }
                status.isUsed = true; // Mark as used when read
            }
        }
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitLambdaExpr(Expr.Lambda expr) {
        resolveCallable(expr, FunctionType.FUNCTION);
        return null;
    }

    @Override
    public Void visitAssignExpr(Expr.Assign expr) {
        resolve(expr.value);
        // When a variable is assigned, it is considered "defined"
        if (!scopes.isEmpty() && scopes.peek().containsKey(expr.name.lexeme())) {
            scopes.peek().get(expr.name.lexeme()).isDefined = true;
            // Also mark as used, as an assignment implies intent to use the variable
            scopes.peek().get(expr.name.lexeme()).isUsed = true; // Assignment is a form of usage
        }
        resolveLocal(expr, expr.name);
        return null;
    }


    private void beginScope() {
        scopes.push(new HashMap<>());
    }

    private void endScope() {
        // Check for unused variables when exiting a scope
        Map<String, VariableStatus> scope = scopes.pop();
        for (Map.Entry<String, VariableStatus> entry : scope.entrySet()) {
            VariableStatus status = entry.getValue();
            if (!status.isUsed) {
                Lox.error(status.declarationToken,
                        "Local variable '" + status.declarationToken.lexeme() + "' is never used.");
            }
        }
    }

    private void declare(Token name) {
        if (scopes.isEmpty()) {
            return;
        }
        Map<String, VariableStatus> scope = scopes.peek();
        if (scope.containsKey(name.lexeme())) {
            Lox.error(name,"Already a variable with this name in this scope.");
        }

        // When declared, it's not yet defined (unless it's a named function, which is defined immediately)
        scope.put(name.lexeme(), new VariableStatus(name, false));
    }

    private void define(Token name) {
        if (scopes.isEmpty()) {
            return;
        }
        // Mark the variable as defined.
        // This is called after the initializer is resolved, or immediately for function names.
        scopes.peek().get(name.lexeme()).isDefined = true;
    }

    private void resolveLocal(Expr expr, Token name) {
        for (int i = scopes.size() - 1; i >= 0 ; i--) {
            if (scopes.get(i).containsKey(name.lexeme())) {
                interpreter.resolve(expr, scopes.size() - 1 - i);
                return;
            }
        }
    }

    void resolve(List<Stmt> statements) {
        for (Stmt statement : statements) {
            resolve(statement);
        }
    }

    private void resolve(Stmt statement) {
        statement.accept(this);
    }

    private void resolve(Expr expr) {
        expr.accept(this);
    }

    private void resolveCallable(FunctionLikeable functionLikeable, FunctionType  type) {
        FunctionType enclosingFunction = currentFunction;
        currentFunction = type;
        beginScope();
        for (Token param : functionLikeable.getParams()) {
            declare(param);
            define(param);
        }
        resolve(functionLikeable.getBody());
        endScope();
        currentFunction = enclosingFunction;
    }
}
