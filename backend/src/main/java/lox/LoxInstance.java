package lox;

import lox.exceptions.RuntimeError;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class LoxInstance {
    private LoxClass currentClass;
    private final Map<String, Object> fields = new HashMap<>();

    LoxInstance(LoxClass currentClass) {
        this.currentClass = currentClass;
    }

    @Override
    public String toString() {
        return currentClass.name + " instance";
    }

    Object get(Interpreter interpreter, Token name) {
        if (fields.containsKey(name.lexeme())) {
            return fields.get(name.lexeme());
        }
        LoxFunction method;
        try {
            method = currentClass.findMethod(name.lexeme());
        } catch (NullPointerException e) {
            throw new RuntimeError(name, "You cannot call a non-static method without first initialising the class!");
        }
        if (method != null) {
            if (method.isGetter()) {
                return method.bind(this).call(interpreter, Collections.emptyList());
            }
            return method.bind(this);
        }
        throw new RuntimeError(name, "Undefined property: '" + name.lexeme() + "'.");
    }

    void set(Token name, Object value) {
        fields.put(name.lexeme(), value);
    }
}
