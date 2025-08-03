package lox;

import lox.exceptions.ReturnException;

import java.util.List;

public class LoxFunction implements LoxCallable {
    // Store parameters and body directly, as they are common to both named and anonymous functions.
    private final List<Token> params;
    private final List<Stmt> body;
    private final Environment closure;
    private final String name; // For named functions; null for anonymous ones.

    // Unified constructor for both named and anonymous functions.
    // The 'name' parameter is null for anonymous functions.
    public LoxFunction(String name, List<Token> params, List<Stmt> body, Environment closure) {
        this.name = name;
        this.params = params;
        this.body = body;
        this.closure = closure;
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
            interpreter.executeBlock(body, environment); // Directly use the stored body list
        } catch (ReturnException returnValue) {
            return returnValue.getValue();
        }

        // If no explicit return, implicitly return nil.
        return null; // Lox 'nil' is Java 'null'
    }

    LoxFunction bind(LoxInstance instance) {
        Environment environment = new Environment(closure);
        environment.define("this", instance);
        return new LoxFunction(this.name, this.params, this.body, environment);
    }

    @Override
    public String toString() {
        if (name == null) {
            return "<fn anonymous>"; // Handle anonymous functions correctly
        }
        return "<fn " + name + ">"; // Use the stored name
    }
}
