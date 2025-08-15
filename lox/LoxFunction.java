package lox;

import lox.exceptions.BreakException;
import lox.exceptions.ContinueException;
import lox.exceptions.ReturnException;

import java.util.List;

public class LoxFunction implements LoxCallable {
    // Store parameters and body directly, as they are common to both named and anonymous functions.
    private final List<Token> params;
    private final List<Stmt> body;
    private final Environment closure;
    private final String name; // For named functions; null for anonymous ones.
    private final boolean isInitializer;
    private final boolean isGetter;

    // Unified constructor for both named and anonymous functions.
    // The 'name' parameter is null for anonymous functions.
    public LoxFunction(String name, List<Token> params, List<Stmt> body, Environment closure, boolean isInitializer) {
        this.name = name;
        this.params = params;
        this.body = body;
        this.closure = closure;
        this.isInitializer = isInitializer;
        this.isGetter = params.isEmpty();
    }

    @Override
    public int arity() {
        return params.size(); // Directly use the stored params list
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        // Each function call gets its own environment.
        // Its enclosing environment is the function's closure (where it was defined).
        Environment environment = new Environment(closure);

        // Bind arguments to parameters in the new environment.
        for (int i = 0; i < params.size(); i++) {
            environment.define(params.get(i).lexeme(), arguments.get(i));
        }

        try {
            interpreter.executeBlock(body, environment);
        } catch (ReturnException returnValue) {
            if (isInitializer) {
                return closure.getAt(0, "this");
            }
            return returnValue.getValue();
        } catch (BreakException breakException) {
            var breakStmt = (Stmt.Break)body.stream().filter(stmt -> stmt instanceof Stmt.Break).findFirst().get();
            Lox.error(breakStmt.keyword,"'break' outside of loop");
        } catch (ContinueException continueException) {
            var continueStmt = (Stmt.Continue)body.stream().filter(stmt -> stmt instanceof Stmt.Continue).findFirst().get();
            Lox.error(continueStmt.keyword,"'continue' outside of loop");
        }
        if (isInitializer) {
            return closure.getAt(0, "this");
        }
        return null;
    }

    LoxFunction bind(LoxInstance instance) {
        Environment environment = new Environment(closure);
        environment.define("this", instance);
        return new LoxFunction(this.name, this.params, this.body, environment, isInitializer);
    }

    @Override
    public String toString() {
        if (name == null) {
            return "<fn anonymous>"; // Handle anonymous functions correctly
        }
        return "<fn " + name + ">"; // Use the stored name
    }

    public boolean isGetter() {
        return isGetter;
    }

    public Environment getClosure() {
        return closure;
    }
}
