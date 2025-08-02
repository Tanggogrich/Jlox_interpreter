package lox;

import lox.exceptions.RuntimeError;

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

    Object get(Token name) {
        if (fields.containsKey(name.lexeme())) {
            return fields.get(name.lexeme());
        }
        throw new RuntimeError(name, "Undefined property: '" + name.lexeme() + "'.");
    }

    void set(Token name, Object value) {
        fields.put(name.lexeme(), value);
    }
}
