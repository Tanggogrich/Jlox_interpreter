package lox;

import java.util.List;
import java.util.Map;

public class LoxClass extends LoxInstance implements LoxCallable{
    final String name;
    final LoxClass superClass;
    private final Map<String, LoxFunction> methods;


    LoxClass(String name, Map<String, LoxFunction> methods,  LoxClass superClass) {
        super(null);
        this.name = name;
        this.methods = methods;
        this.superClass = superClass;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int arity() {
        LoxFunction initializer = findMethod("init");
        if (initializer == null) {
            return 0;
        }
        return initializer.arity();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        LoxInstance instance = new LoxInstance(this);
        LoxFunction initializer = findMethod("init");
        if (initializer != null) {
            initializer.bind(instance).call(interpreter, arguments);
        }
        return instance;
    }

    LoxFunction findMethod(String name) {
        if (methods.containsKey(name)) {
            return methods.get(name);
        }

        if (superClass != null) {
            return superClass.findMethod(name);
        }
        return null;
    }

    void defineStaticMethod(Token name, Object value) {
        super.set(name, value);
    }
}
