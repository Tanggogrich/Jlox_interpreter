package lox;

import lox.exceptions.RuntimeError;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    private final Environment enclosing;
    private final Map<String, Object> values = new HashMap<>();

    public Environment() {
        this.enclosing = null;
    }

    public Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    public Object get(Token name) {
        if (values.containsKey(name.lexeme())) {
            if (values.get(name.lexeme()) == null) {
                throw new RuntimeError(name, "Uninitialized variable " + name.lexeme() + ". Please assign this variable before calling it.");
            }
            return values.get(name.lexeme());
        }

        if (enclosing != null) {
            return enclosing.get(name);
        }

        throw new RuntimeError(name,
                "Undefined variable '" + name.lexeme() + "'");
    }

    public void define(String name, Object value) {
        values.put(name, value);
    }

    Object getAt(int distance, String name) {
        return ancestor(distance).values.get(name);
    }

    void assignAt(int distance, Token name, Object value) {
        ancestor(distance).values.put(name.lexeme(), value);
    }

    // We know exactly which environment in the chain will have the variable.
    // We reach it using this helper method.
    // This walks a fixed number of hops up the parent chain and returns the environment there.
    Environment ancestor(int distance) {
        Environment environment = this;
        for (int i = 0; i < distance; i++) {
            environment = environment != null ? environment.enclosing : null;
        }
        return environment;
    }

    public void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme())) {
            values.put(name.lexeme(), value);
            return;
        }

        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        throw new RuntimeError(name,
                "Undefined variable '" + name.lexeme() + "'");
    }
}
