package lox;

import java.util.List;

abstract public class Expr {
    interface Visitor<R> {
        R visitBinaryExpr(Binary expr);

        R visitGroupingExpr(Grouping expr);

        R visitLiteralExpr(Literal expr);

        R visitUnaryExpr(Unary expr);

        // reverse Polish notation (RPN)
        R visitBinaryRPNExpr(BinaryRPN expr);

        // ternary expression (a ? b : c)
        R visitTernaryExpr(Ternary expr);
    }

    public static class Binary extends Expr {
        Binary(Expr left, Token operator, Expr right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitBinaryExpr(this);
        }

        final Expr left;
        final Token operator;
        final Expr right;
    }

    // additional form for binary operator expression - reverse Polish notation (RPN)
    public static class BinaryRPN extends Expr {
        BinaryRPN(Expr left, Token operator, Expr right) {
            this.left = left;
            this.right = right;
            this.operator = operator;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitBinaryRPNExpr(this);
        }

        final Expr left;
        final Expr right;
        final Token operator;
    }

    public static class Grouping extends Expr {
        Grouping(Expr expression) {
            this.expression = expression;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitGroupingExpr(this);
        }

        final Expr expression;
    }

    public static class Literal extends Expr {
        Literal(Object value) {
            this.value = value;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitLiteralExpr(this);
        }

        final Object value;
    }

    public static class Unary extends Expr {
        Unary(Token operator, Expr right) {
            this.operator = operator;
            this.right = right;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitUnaryExpr(this);
        }

        final Token operator;
        final Expr right;
    }

    public static class Ternary extends Expr {
        final Expr condition;
        final Expr thenBranch;
        final Expr elseBranch;

        Ternary(Expr condition, Expr thenBranch, Expr elseBranch) {
            this.condition = condition;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitTernaryExpr(this);
        }
    }

    abstract <R> R accept(Visitor<R> visitor);
}
