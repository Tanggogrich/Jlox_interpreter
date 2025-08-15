package lox;

import lox.exceptions.BreakException;
import lox.exceptions.ContinueException;
import lox.exceptions.ReturnException;
import lox.exceptions.RuntimeError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static lox.Expr.*;
import static lox.Stmt.*;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    final Environment globals = new Environment();
    private Environment environment = globals;
    private final Map<Expr, Integer> locals = new HashMap<>();

    Interpreter() {
        globals.define("clock", new LoxCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return (double) System.currentTimeMillis() / 1000.0;
            }

            @Override
            public String toString() {
                return "<native fn>";
            }
        });
    }

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error, this.getClass());
        }
    }

    @Override
    public Void visitExpressionStmt(Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    /*
    Take a function syntax node — a compile-time representation of the function — and convert it to its runtime representation.
    LoxFunction wraps the syntax node.
    */
    @Override
    public Void visitFunctionStmt(Function stmt) {
        // Pass the function's name, parameters, body, and closure environment
        LoxFunction function = new LoxFunction(stmt.name.lexeme(), stmt.params, stmt.body, environment, false);
        environment.define(stmt.name.lexeme(), function);
        return null;
    }

    @Override
    public Void visitIfStmt(If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitPrintStmt(Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitReturnStmt(Return stmt) {
        Object value = null;
        if (stmt.value != null) {
            value = evaluate(stmt.value);
        }
        throw new ReturnException(value);
    }

    @Override
    public Void visitVarStmt(Var stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }

        environment.define(stmt.name.lexeme(), value);
        return null;
    }

    @Override
    public Void visitWhileStmt(While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            try {
                execute(stmt.body); // Execute the loop body
            } catch (BreakException breakExc) {
                // Caught a break statement, exit the loop entirely.
                break;
            } catch (ContinueException continueExc) {
                // Caught a continue statement, skip to the next iteration.
                // The 'while' loop's condition will be re-evaluated.
                continue;
            }
        }
        return null;
    }

    @Override
    public Void visitBreakStmt(Break stmt) {
        // Throw a special exception to signal breaking out of the loop.
        throw new BreakException();
    }

    @Override
    public Void visitContinueStmt(Continue stmt) {
        // Throw a special exception to signal continuing to the next iteration.
        throw new ContinueException();
    }

    @Override
    public Object visitAssignExpr(Assign expr) {
        Object value = evaluate(expr.value);
        Integer distance = locals.get(expr);
        if (distance != null) {
            environment.assignAt(distance, expr.name, value);
        } else {
            globals.assign(expr.name, value);
        }
        return value;
    }

    @Override
    public Object visitLiteralExpr(Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitLogicalExpr(Logical expr) {
        Object left = evaluate(expr.left);

        if (expr.operator.tokenType() == TokenType.OR) {
            if (isTruthy(left)) {
                return left;
            }
        } else {
            if (!isTruthy(left)) {
                return left;
            }
        }
        return evaluate(expr.right);
    }

    @Override
    public Object visitSetExpr(Set expr) {
        Object object = evaluate(expr.object);

        if (!(object instanceof LoxInstance)) {
            throw new RuntimeError(expr.name, "Only instances have fields.");
        }

        Object value = evaluate(expr.value);
        ((LoxInstance) object).set(expr.name, value);
        return value;
    }

    @Override
    public Object visitThisExpr(This expr) {
        return lookUpVariable(expr.keyword, expr);
    }

    @Override
    public Object visitSuperExpr(Super expr) {
        int distance = locals.get(expr);
        LoxClass superClass = (LoxClass) environment.getAt(distance, "super");
        LoxInstance object = (LoxInstance) environment.getAt(distance - 1, "this");
        LoxFunction method = superClass.findMethod(expr.method.lexeme());
        if (method == null) {
            throw new RuntimeError(expr.method, "Undefined property '" + expr.method.lexeme() + "'.");
        }
        return method.bind(object);
    }

    @Override
    public Object visitUnaryExpr(Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.tokenType()) {
            case BANG:
                return !isTruthy(right);
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double) right;
        }

        return null;
    }

    @Override
    public Object visitCallExpr(Call expr) {
        Object callee = evaluate(expr.callee);

        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }

        if (!(callee instanceof LoxCallable)) {
            throw new RuntimeError(expr.paren, "Can only call functions and classes.");
        }

        LoxCallable function = (LoxCallable) callee;
        if (arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren, "Expected " +
                    function.arity() + " arguments but got " +
                    arguments.size() + ".");
        }
        return function.call(this, arguments);
    }

    @Override
    public Object visitGetExpr(Get expr) {
        Object object = evaluate(expr.object);
        if (object instanceof LoxInstance) {
            return ((LoxInstance) object).get(this, expr.name);
        }

        throw new RuntimeError(expr.name, "Only instances have properties.");
    }

    @Override
    public Object visitBinaryRPNExpr(BinaryRPN expr) {
        return visitBinaryExpr(new Binary(expr.left, expr.operator, expr.right));
    }

    @Override
    public Object visitTernaryExpr(Ternary expr) {
        Object condition = evaluate(expr.condition);

        if (isTruthy(condition)) {
            return evaluate(expr.thenBranch);
        } else {
            return evaluate(expr.elseBranch);
        }
    }

    @Override
    public Object visitVariableExpr(Variable expr) {
        return lookUpVariable(expr.name, expr);
    }

    @Override
    public Object visitLambdaExpr(Lambda expr) {
        // For lambdas, the name is null
        return new LoxFunction(null, expr.params, expr.body, environment, false);
    }

    @Override
    public Object visitBinaryExpr(Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.tokenType()) {
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double) left > (double) right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left < (double) right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left >= (double) right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left <= (double) right;
            case BANG_EQUAL:
                return !isEqual(left, right);
            case EQUAL_EQUAL:
                return isEqual(left, right);
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left - (double) right;
            case PLUS: {
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }

                if (left instanceof String && right instanceof String) {
                    return (String) left + (String) right;
                }

                if (left instanceof String && right instanceof Double) {
                    return (String) left + stringify(right);
                }

                if (left instanceof Double && right instanceof String) {
                    return stringify(left) + right;
                }

                throw new RuntimeError(expr.operator,
                        "Operands must be two numbers or two strings.");
            }
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                if ((double) right == 0.0) {
                    throw new RuntimeError(expr.operator, "Division by zero");
                }
                return (double) left / (double) right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double) left * (double) right;
        }

        // Unreachable
        return null;
    }

    @Override
    public Object visitGroupingExpr(Grouping expr) {
        return evaluate(expr.expression);
    }

    private Object lookUpVariable(Token name, Expr expr) {
        Integer distance = locals.get(expr);
        if (distance != null) {
            return environment.getAt(distance, name.lexeme());
        } else {
            return globals.get(name);
        }
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) {
            return;
        }
        throw new RuntimeError(operator, "Operand must be a number");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) {
            return;
        }
        throw new RuntimeError(operator, "Operands must be a numbers");
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    void resolve(Expr expr, int depth) {
        locals.put(expr, depth);
    }

    void executeBlock(List<Stmt> statements,
                      Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;

            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    @Override
    public Void visitBlockStmt(Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        Object superClass = null;
        if (stmt.superClass != null) {
            superClass = evaluate(stmt.superClass);
            if (!(superClass instanceof LoxClass)) {
                throw new RuntimeError(stmt.superClass.name, "Superclass must be a class");
            }
        }

        environment.define(stmt.name.lexeme(), null);

        if (stmt.superClass != null) {
            environment = new Environment(environment);
            environment.define("super", superClass);
        }

        Map<String, LoxFunction> instanceMethods = new HashMap<>();
        List<Function> staticMethods = new ArrayList<>();

        for (Stmt.Function method : stmt.methods) {
            if (method.isStatic) {
                staticMethods.add(method);
            } else {
                LoxFunction function = new LoxFunction(
                        method.name.lexeme(),
                        method.params,
                        method.body, environment,
                        method.name.lexeme().equals("init"));
                instanceMethods.put(method.name.lexeme(), function);
            }
        }
        LoxClass loxClass = new LoxClass(stmt.name.lexeme(), instanceMethods, (LoxClass) superClass);

        for (Function staticMethod : staticMethods) {
            LoxFunction function = new LoxFunction(
                    staticMethod.name.lexeme(),
                    staticMethod.params,
                    staticMethod.body,
                    environment,
                    false);
            loxClass.defineStaticMethod(staticMethod.name, function);
        }

        if (stmt.superClass != null) {
            environment = environment.getEnclosing();
        }
        environment.assign(stmt.name, loxClass);
        return null;
    }

    private boolean isTruthy(Object object) {
        if (object == null) {
            return false;
        }
        if (object instanceof Boolean) {
            return (boolean) object;
        }
        return true;
    }

    private boolean isEqual(Object left, Object right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null) {
            return false;
        }

        return left.equals(right);
    }

    private String stringify(Object object) {
        if (object == null) {
            return "nil";
        }

        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return object.toString();
    }
}
