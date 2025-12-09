package com.peck;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.peck.TokenType.*;

public class Parser {

    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 1; // index 0 is SOF

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public List<Stmt> parse() {
        List<Stmt> stmts = new ArrayList<>();
        while(!isAtEnd()) stmts.add(declaration());
        return stmts;
    }

    /**
     * declaration ->   varDeclaration | funDeclaration | statement
     */
    private Stmt declaration() {
        try {
            if(consumeIfMatchAny(FUN)) return funDeclaration();
            if(consumeIfMatchAny(VAR)) return varDeclaration();
            return statement();
        } catch (ParseError e) {
            synchronize();
            return null;
        }
    }

    private Stmt varDeclaration() {
        consume(IDENTIFIER,"Expect variable name.");
        Token name = previous();
        Expr initializer = null;
        if(consumeIfMatchAny(EQUAL)) initializer = expression();
        consume(SEMICOLON,"Expect ';' at end of statement");
        return new Stmt.VarDeclaration(name, initializer);
    }

    private Stmt funDeclaration() {
        // name
        consume(IDENTIFIER,"Expect function name.");
        Token name = previous();

        //params
        List<Token> params = new ArrayList<>();
        consume(LEFT_PAREN,"Expect '(' after 'function'.");
        if(peek().getType() != RIGHT_PAREN) {
            do{
                if(params.size() >= 255)
                    error(peek(), "Can't have more than 255 parameters.");

                consume(IDENTIFIER, "Expect param name.");
                params.add(previous());
            }while(consumeIfMatchAny(COMMA));
        }
        consume(RIGHT_PAREN,"Expect ')' after 'function'.");

        //body
        consume(LEFT_BRACE,"Expect '{' before function body.");
        Stmt.Block body = (Stmt.Block) blockStatement();

        return new Stmt.Function(name, params, body); 
    }

    /**
     * statement ->   exprStmt | printStmt | ifStmt | whileStmt | for | block
     */
    private Stmt statement() {
        if(consumeIfMatchAny(IF)) return ifStatement();
        if(consumeIfMatchAny(WHILE)) return whileStatement();
        if(consumeIfMatchAny(FOR)) return forStatement();
        if(consumeIfMatchAny(PRINT)) return printStatement();
        if(consumeIfMatchAny(LEFT_BRACE)) return blockStatement();

        return expressionStatement();
    }


    /**
     * for -> 'for' '(' (varDeclaration | exprStatement | ';')
     *        expression? ';'
     *        expression?   
     *        ')' statement
     */
    private Stmt forStatement() {
        consume(LEFT_PAREN,"Expect '(' after 'for'.");
        
        Stmt initializer;
        if(consumeIfMatchAny(SEMICOLON)) {
            initializer = null;
        } else if(consumeIfMatchAny(VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        Expr condition = new Expr.Literal(true);
        if(peek().getType() != SEMICOLON) {
            condition = expression();
        }
        consume(SEMICOLON, "Expect ';' after loop condition.");

        Expr increment = null;
        if(peek().getType() != RIGHT_PAREN) {
            increment = expression();
        }

        consume(RIGHT_PAREN,"Expect ')' after 'for'.");

        Stmt body = statement();

        if(increment != null) {
            body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
        }

        body = new Stmt.While(condition, body);

        if(initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }
        return body;
    }


    /**
     * while -> 'while' '(' expression ')' statement
     */
    private Stmt whileStatement() {
        consume(LEFT_PAREN,"Expect '(' after 'while'.");
        Expr condition = expression();
        consume(RIGHT_PAREN,"Expect ')' after 'while'.");

        Stmt body = statement();
        return new Stmt.While(condition, body);
    }

    /**
     * if -> 'if' '(' expression ')' statement ('else' statement)?
     */
    private Stmt ifStatement() {
        consume(LEFT_PAREN,"Expect '(' after 'if'.");
        Expr condition = expression();
        consume(RIGHT_PAREN,"Expect ')' after 'if'.");

        Stmt thenStmt = statement();
        Stmt elseStmt = null;
        if(consumeIfMatchAny(ELSE)) {
            elseStmt= statement();
        }
        return new Stmt.If(condition, thenStmt, elseStmt);
    }

    /**
     * block -> '{' declaration* '}'
     */
    private Stmt blockStatement() {
        List<Stmt> stmts = new ArrayList<>();

        while(peek().getType() != RIGHT_BRACE && !isAtEnd())
            stmts.add(declaration());

        consume(RIGHT_BRACE, "Expect '}' after block.");

        return new Stmt.Block(stmts);
    }

    /**
     * printStmt -> 'print' expression ';'
     */
    private Stmt printStatement() {
        Expr expr = expression();
        consume(SEMICOLON,"Expect ';' at end of statement");
        return new Stmt.Print(expr);
    }

    /**
     * exprStmt -> expression ';'
     */
    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON,"Expect ';' at end of statement");
        return new Stmt.Expression(expr);
    }

    /**
     *  expression -> assignment
     */
    private Expr expression() {
        return assignment();
    }

    /**
     *  assignment -> IDENTIFIER "=" assignment
     *                | logic_or
     */
    private Expr assignment() {
        Expr left = or();

        if(consumeIfMatchAny(EQUAL)) {
            Token operator = previous();

            // assignment is right-associative
            // we don't loop to build expression same as Binary
            Expr val = assignment();

            if(left instanceof Expr.Variable var) {
                Token name = var.name;
                return new Expr.Assign(name, val);
            }

            throw error(operator, "Invalid assignment target.");
        }

        return  left;
    }

    /**
     * logic_or -> logic_and ('or' logic_and)*
     */
    private Expr or() {
        Expr expr = and();

        while(consumeIfMatchAny(OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    /**
     * logic_and -> equality ('or' equality)*
     */
    private Expr and() {
        Expr expr = equality();

        while(consumeIfMatchAny(AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
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
     * unary -> ('!'|'-') unary | call
     */
    private Expr unary() {
        if(consumeIfMatchAny(BANG,MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return call();
    }

    /**
     * call -> primary ( '(' arguments? ')' )*
     */
    private Expr call() {
        Expr expr = primary();
        
        if(consumeIfMatchAny(LEFT_PAREN)) {
            List<Expr> args = Collections.emptyList();
            if(peek().getType() != RIGHT_PAREN) {
                args = arguments();
            }
            consume(RIGHT_PAREN,"Expect ')' at the end of function.");
            return new Expr.Call(expr, previous(), args);
        }

        return expr;
    }

    /**
     * arguments -> expression (',' expression)*
     */
    private List<Expr> arguments() {
        List<Expr> args = new ArrayList<>();
        
        do {
            if (args.size() >= 255) {
                error(peek(), "Can't have more than 255 arguments.");
            }
            args.add(expression());
        }while(consumeIfMatchAny(COMMA));
        
        return args;
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

        // A function also is stored in env as a variable
        if(consumeIfMatchAny(IDENTIFIER))
            return new Expr.Variable(previous()); 

        throw error(peek(), "Expect expression");
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
                default:
                    consume(); // Consume rest token in the erroneous expression
            }
        }

    }

}
