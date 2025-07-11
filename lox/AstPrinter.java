package lox;

public class AstPrinter implements Expr.Visitor<String> {

    /*public static void main(String[] args) {
        Expr expression = new Expr.BinaryRPN(
                new Expr.BinaryRPN(
                        new Expr.Literal(1),
                        new Token(TokenType.STAR, "+", null, 1),
                        new Expr.Literal(2)
                ),
                new Token(TokenType.STAR, "*", null, 1),
                new Expr.BinaryRPN(
                        new Expr.Literal(4),
                        new Token(TokenType.STAR, "-", null, 1),
                        new Expr.Literal(3)
                )
        );

        System.out.println(new AstPrinter().print(expression));
    }
    */
    String print(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return parenthesize(expr.operator.lexeme(), expr.left, expr.right);
    }

    @Override
    public String visitBinaryRPNExpr(Expr.BinaryRPN expr) {
        return transformToRPN(expr.operator.lexeme(), expr.left, expr.right);
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return parenthesize("group", expr.expression);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if (expr.value == null) {
            return "nil";
        }
        return expr.value.toString();
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return parenthesize(expr.operator.lexeme(), expr.right);
    }

    @Override
    public String visitTernaryExpr(Expr.Ternary expr) {
        return parenthesize("?:", expr.condition, expr.thenBranch, expr.elseBranch);
    }

    private String parenthesize(String name, Expr... exprs) {
        StringBuilder sb = new StringBuilder();

        sb.append("(").append(name);
        for (Expr expr : exprs) {
            sb.append(" ");
            sb.append(expr.accept(this));
        }
        sb.append(")");

        return sb.toString();
    }

    private String transformToRPN(String name, Expr... exprs) {
        StringBuilder sb = new StringBuilder();

        for (Expr expr : exprs) {
            sb.append(expr.accept(this));
            sb.append(" ");
        }
        sb.append(name);

        return sb.toString();
    }
}
