package com.peck;

import java.util.List;

import static com.peck.TokenType.*;

public class Parser {

    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 1; // index 0 is SOF

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public Expr parse() {
        try {
            return expression();
        } catch(ParseError e) {
            return null;
        }
    }

    /**
     *  expression = equality
     */
    private Expr expression() {
        return equality();
    }

    /**
     * equality -> comparison (('=='|'!=') comparison)*
     */
    private Expr equality() {
        Expr expr = comparison();

        while (consumeIfMatchAny(BANG_EQUAL,EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * comparison -> term (('>'|'<'|'<='|'>=') term) *
     */
    private Expr comparison() {
        Expr expr = term();

        while (consumeIfMatchAny(GREATER,GREATER_EQUAL,LESS,LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * term -> factor (('+'|'-') factor)*
     */
    private Expr term() {
        Expr expr = factor();

        while (consumeIfMatchAny(PLUS,MINUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * factor -> unary (('/'|'*') unary)*
     */
    private Expr factor() {
        Expr expr = unary();

        while (consumeIfMatchAny(STAR,SLASH)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * unary -> ('!'|'-') unary | primary
     */
    private Expr unary() {
        if(consumeIfMatchAny(BANG,MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return primary();
    }

    /**
     * primary -> NUMBER |STRING|true|false|nil|'(' expression ')'
     */
    private Expr primary() {
        if(consumeIfMatchAny(NUMBER,STRING))
            return new Expr.Literal(previous().getLiteral());

        if(consumeIfMatchAny(TRUE))
            return new Expr.Literal(true);
        if(consumeIfMatchAny(FALSE))
            return new Expr.Literal(false);

        if(consumeIfMatchAny(NIL))
            return new Expr.Literal(null);

        if(consumeIfMatchAny(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression");
            return expr;
        }

        throw error(peek(), "Expect expression");
    }


    private boolean isAtStart() {
        return previous().getType() == SOF;
    }

    private boolean isAtEnd() {
        return peek().getType() == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current-1);
    }

    private void consume() {
        // If we exhaust tokens , the peek() will only return EOF instead of NULL
        if(!isAtEnd())
            current++;
    }

    private boolean consumeIfMatchAny(TokenType... types) {
        if(isAtEnd())
            return false;

        Token token = peek();
        for(TokenType type : types) {
            assert token != null;
            if(token.getType() == type) {
               consume();
               return true;
            }
        }
        return false;
    }


    private void consume(TokenType type,String errorMessage) {
        // The type of next token must be the type we expect.
        if(peek().getType() == type) {
            consume();
        } else {
            throw error(peek(),errorMessage);
        }
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }


    // we need to discard rest tokens what is a part of an erroneous expression
    // when an error is thrown during parsing
    private void synchronize() {

        while(!isAtEnd()) {
            Token next = peek();

            if(next.getType() == SEMICOLON) {
                consume();
                return;
            }

            switch(next.getType()) {
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

            consume(); // Consume rest token in the erroneous expression
        }

    }

}
