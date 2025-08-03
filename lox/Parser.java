package lox;

import lox.exceptions.ParseError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static lox.Expr.*;
import static lox.Stmt.*;
import static lox.TokenType.*;

public class Parser {
    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    /// /////////////////   PARSER EXPRESSION AND STATEMENT GRAMMAR   ////////////////////////////////

    private Expr expression() {
        return assignmentOrTernary();
    }

    private Stmt declaration() {
        try {
            if (match(CLASS)) {
                return classDeclaration();
            }
            if (match(FUN)) {
                return function("function");
            }
            if (match(VAR)) {
                return varDeclaration();
            }
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt statement() {
        if (match(FOR)) {
            return forStatement();
        }
        if (match(IF)) {
            return ifStatement();
        }
        if (match(PRINT)) {
            return printStatement();
        }
        if (match(RETURN)) {
            return returnStatement();
        }
        if (match(WHILE)) {
            return whileStatement();
        }
        if (match(BREAK)) {
            return breakStatement();
        }
        if (match(CONTINUE)) {
            return continueStatement();
        }
        if (match(LEFT_BRACE)) {
            return new Block(block());
        }
        return expressionStatement();
    }

    /////////////////////////////// STATEMENTS ////////////////////////////////////////

    private Stmt forStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'for'.");

        Stmt initializer;
        if (match(SEMICOLON)) {
            initializer = null;
        } else if (match(VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        Expr condition = null;
        if (!check(SEMICOLON)) {
            condition = expression();
        }
        consume(SEMICOLON, "Expect ';' after loop condition.");

        Expr increment = null;
        if (!check(RIGHT_PAREN)) {
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expect ')' after loop clauses.");

        Stmt body = statement();
        if (initializer != null) {
            body = new Block(Arrays.asList(body, new Expression(increment)));
        }

        if (condition == null) {
            condition = new Literal(true);
        }
        body = new While(condition, body);

        if (initializer != null) {
            body = new Block(Arrays.asList(initializer, body));
        }

        return body;
    }

    private Stmt ifStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after 'if'.");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE)) {
            elseBranch = statement();
        }

        return new If(condition, thenBranch, elseBranch);
    }

    //TODO: remove print statement after implementing function calls
    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Print(value);
    }

    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;
        if (!check(SEMICOLON)) {
            value = expression();
        }
        consume(SEMICOLON, "Expect ';' after return value.");
        return new Return(keyword, value);
    }

    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expect variable name.");

        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }

        consume(SEMICOLON, "Expect ';' after variable declaration.");
        return new Var(name, initializer);
    }

    private Stmt classDeclaration() {
        Token name = consume(IDENTIFIER, "Expect class name.");
        consume(LEFT_BRACE, "Expect '{' before class body.");

        List<Stmt.Function> methods = new ArrayList<>();
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            methods.add(function("method"));
        }

        consume(RIGHT_BRACE, "Expect '}' after class body.");
        return new Stmt.Class(name, methods);
    }

    private Stmt whileStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after 'while'.");
        Stmt body = statement();

        return new While(condition, body);
    }

    private Stmt breakStatement() {
        Token keyword = previous();
        consume(SEMICOLON, "Expect ';' after break.");
        return new Break(keyword);
    }

    private Stmt continueStatement() {
        Token keyword = previous();
        consume(SEMICOLON, "Expect ';' after continue.");
        return new Continue(keyword);
    }

    private Stmt expressionStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Expression(value);
    }

    private Function function(String kind) {
        Token name = consume(IDENTIFIER, "Expect " + kind + " name.");

        consume(LEFT_PAREN, "Expect '(' after " + kind + ".");
        List<Token> parameters = parameters();
        consume(RIGHT_PAREN, "Expect ')' after parameters.");

        //Parse the body and wrap it all up in a function node
        consume(LEFT_BRACE, "Expect '{' before " + kind + " body.");
        List<Stmt> body = block();
        return new Function(name, parameters, body);
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    ////////////////////////////////// EXPRESSIONS /////////////////////////////////////////////

//    private Expr comma() {
//        return parseBinaryLeftAssociative(this::assignmentOrTernary, COMMA);
//    }

    private Expr assignmentOrTernary() {
        Expr expr = or();

        if (match(QUESTION_MARK)) {
            Expr thenBranch = expression();
            consume(COLON, "Expect ':' after then branch of conditional expression");
            Expr elseBranch = assignmentOrTernary();
            expr = new Ternary(expr, thenBranch, elseBranch);
        }

        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignmentOrTernary();

            if (expr instanceof Variable) {
                Token name = ((Variable) expr).name;
                return new Assign(name, value);
            } else if (expr instanceof Get get) {
                return new Set(get.object, get.name, value);
            }

            error(equals, "Invalid assignment target.");
        }
        return expr;
    }

    private Expr or() {
        return parseLogicalExpression(this::and, OR);
    }

    private Expr and() {
        return parseLogicalExpression(this::equality, AND);
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
            return new Unary(operator, right);
        }
        return call();
    }

    private Expr call() {
        Expr expr = primary();

        while (true) {
            if (match(LEFT_PAREN)) {
                expr = finishCall(expr);
            } else if (match(DOT)) {
                Token name = consume(IDENTIFIER, "Expect property name after '.'.");
                expr = new Get(expr, name);
            } else {
                break;
            }
        }

        return expr;
    }

    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (arguments.size() >= 255) {
                    error(peek(), "Can't have more than 255 arguments.");
                }
                arguments.add(expression());
            } while (match(COMMA));
        }

        Token paren = consume(RIGHT_PAREN, "Expect ')' after arguments.");
        return new Call(callee, paren, arguments);
    }

    private Expr primary() {
        if (match(FALSE)) {
            return new Literal(false);
        }

        if (match(TRUE)) {
            return new Literal(true);
        }

        if (match(NIL)) {
            return new Literal(null);
        }

        if (match(THIS)) {
            return new This(previous());
        }

        if (match(IDENTIFIER)) {
            return new Variable(previous());
        }

        if (match(NUMBER, STRING)) {
            return new Literal(previous().literal());
        }

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Grouping(expr);
        }

        // Error productions for binary operators without left operand
        if (match(COMMA)) {
            throw error(previous(), "Missing left-hand operand for comma operator.");
        }

        if (match(BANG_EQUAL, EQUAL_EQUAL)) {
            throw error(previous(), "Missing left-hand operand for equality operator.");
        }

        if (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            throw error(previous(), "Missing left-hand operand for comparison operator.");
        }

        if (match(PLUS)) {
            throw error(previous(), "Missing left-hand operand for addition operator.");
        }

        if (match(SLASH, STAR)) {
            throw error(previous(), "Missing left-hand operand for multiplication/division operator.");
        }

        if (match(FUN)) {
            return lambdaExpression();
        }

        throw error(peek(), "Expect expression!");
    }

    private Expr lambdaExpression() {
        consume(LEFT_PAREN, "Expect '(' after 'fun'.");
        List<Token> parameters = parameters();
        consume(RIGHT_PAREN, "Expect ')' after parameters.");
        consume(LEFT_BRACE, "Expect '{' before function body.");
        List<Stmt> body = block();
        return new Expr.Lambda(parameters, body);
    }

    ///////////////////////////////   HELPER METHODS   ////////////////////////////////


    /**
     *The code for handling arguments in a call, except not split out into a helper method.
     *The outer if statement handles the zero-parameter case,
     *and the inner while loop parses parameters as long as we find commas to separate them.
     * @return The result is the list of tokens for each parameter’s name.
     **/
    private List<Token> parameters() {
        List<Token> parameters = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (parameters.size() >= 255) {
                    error(peek(), "Can't have more than 255 parameters.");
                }

                parameters.add(consume(IDENTIFIER, "Expect parameter name."));
            } while (match(COMMA));
        }
        return parameters;
    }

    /**
     * Helper method for parsing left-associative binary operators.
     *
     * @param operandParser A supplier that parses the next higher precedence level
     * @param operators     The token types for the operators at this precedence level
     * @return The parsed expression
     */
    private Expr parseBinaryLeftAssociative(Supplier<Expr> operandParser, TokenType... operators) {
        Expr expr = operandParser.get();
        while (match(operators)) {
            Token operator = previous();
            Expr right = operandParser.get();
            expr = new Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr parseLogicalExpression(Supplier<Expr> operandParser, TokenType... operators) {
        Expr expr = operandParser.get();
        while (match(operators)) {
            Token operator = previous();
            Expr right = operandParser.get();
            expr = new Logical(expr, operator, right);
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

        while (!isAtEnd()) {
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
