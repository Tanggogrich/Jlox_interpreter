package lox;

import java.util.List;
import java.util.function.Supplier;

import static lox.TokenType.*;

public class Parser {
    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    Expr parse() {
        try {
            return expression();
        } catch (ParseError error) {
            return null;
        }
    }

    ////////////////////////////////   PARSER EXPRESSION GRAMMAR   ////////////////////////////////

    private Expr expression() {
        return comma();
    }

    private Expr comma() {
        return parseBinaryLeftAssociative(this::ternary, COMMA);
    }

    private Expr ternary() {
        Expr expr = equality();

        if (match(QUESTION_MARK)) {
            Expr thenBranch = expression();
            consume(COLON, "Expect ':' after then branch of conditional expression");
            Expr elseBranch = ternary();
            expr = new Expr.Ternary(expr, thenBranch, elseBranch);
        }

        return expr;
    }

    private Expr equality() {
        return parseBinaryLeftAssociative(this::comparison, BANG_EQUAL, EQUAL_EQUAL);
    }

    private Expr comparison() {
        return parseBinaryLeftAssociative(this::term, GREATER, GREATER_EQUAL, LESS, LESS_EQUAL);
    }

    private Expr term() {
        return parseBinaryLeftAssociative(this::factor, MINUS, PLUS);
    }

    private Expr factor() {
        return parseBinaryLeftAssociative(this::unary, SLASH, STAR);
    }

    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return primary();
    }

    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal());
        }

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        // Error productions for binary operators without left operand
        if (match(COMMA)) {
            error(previous(), "Missing left-hand operand for comma operator.");
            comma();
            return null;
        }

        if (match(BANG_EQUAL, EQUAL_EQUAL)) {
            error(previous(), "Missing left-hand operand for equality operator.");
            equality();
            return null;
        }

        if (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            error(previous(), "Missing left-hand operand for comparison operator.");
            comparison();
            return null;
        }

        if (match(PLUS)) {
            error(previous(), "Missing left-hand operand for addition operator.");
            term();
            return null;
        }

        if (match(SLASH, STAR)) {
            error(previous(), "Missing left-hand operand for multiplication/division operator.");
            factor();
            return null;
        }

        throw error(peek(), "Expect expression!");
    }

    ///////////////////////////////   HELPER METHODS   ////////////////////////////////

    /**
     * Helper method for parsing left-associative binary operators.
     * @param operandParser A supplier that parses the next higher precedence level
     * @param operators The token types for the operators at this precedence level
     * @return The parsed expression
     */
    private Expr parseBinaryLeftAssociative(Supplier<Expr> operandParser, TokenType... operators) {
        Expr expr = operandParser.get();
        while (match(operators)) {
            Token operator = previous();
            Expr right = operandParser.get();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType tokenType, String message) {
        if (check(tokenType)) {
            return advance();
        }
        throw error(peek(), message);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) {
            return false;
        }
        return peek().tokenType() == type;
    }

    private Token advance() {
        if (!isAtEnd()) {
            current++;
        }
        return previous();
    }

    private boolean isAtEnd() {
        return peek().tokenType() == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError(message);
    }

    private void synchronize() {
        advance();

        while(!isAtEnd()) {
            if (previous().tokenType() == SEMICOLON) {
                switch (peek().tokenType()) {
                    case CLASS:
                    case FUN:
                    case VAR:
                    case FOR:
                    case IF:
                    case WHILE:
                    case PRINT:
                    case RETURN:
                        return;
                }

                advance();
            }
        }
    }
}
