package lox;

import lox.exceptions.ReturnException;

import java.util.List;

public class LoxFunction implements LoxCallable {
    private final Stmt.Function declaration;

    public LoxFunction(Stmt.Function declaration) {
        this.declaration = declaration;
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        // Each function gets its own environment where it stores those variables.
        Environment environment = new Environment(interpreter.globals);

        /*
        Each function call gets its own environment. Otherwise, recursion would break.
        If there are multiple calls to the same function in play at the same time, each needs its own environment,
        even though they are all calls to the same function.
        */
        for (int i = 0; i < declaration.params.size(); i++) {
            environment.define(declaration.params.get(i).lexeme(), arguments.get(i));
        }

        try {
            interpreter.executeBlock(declaration.body, environment);
        } catch (ReturnException returnValue) {
            return returnValue.getValue();
        }
        return null;
    }

    @Override
    public String toString() {
        return "<fn "+ declaration.name.lexeme() + ">";
    }
}
