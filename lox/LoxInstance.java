package lox;

public class LoxInstance {
    private final LoxClass currentClass;

    LoxInstance(LoxClass currentClass) {
        this.currentClass = currentClass;
    }

    @Override
    public String toString() {
        return currentClass.name + " instance";
    }
}
