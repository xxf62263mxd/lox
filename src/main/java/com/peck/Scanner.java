package com.peck;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.peck.TokenType.*;

public class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();

    private int start  = 0;
    private int current = 0;
    private int line = 1;

    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("and",    AND);
        keywords.put("class",  CLASS);
        keywords.put("else",   ELSE);
        keywords.put("false",  FALSE);
        keywords.put("for",    FOR);
        keywords.put("fun",    FUN);
        keywords.put("if",     IF);
        keywords.put("nil",    NIL);
        keywords.put("or",     OR);
        keywords.put("print",  PRINT);
        keywords.put("return", RETURN);
        keywords.put("super",  SUPER);
        keywords.put("this",   THIS);
        keywords.put("true",   TRUE);
        keywords.put("var",    VAR);
        keywords.put("while",  WHILE);
    }

    public Scanner(String source) {
        this.source = source;
    }

    public List<Token> scanTokens() {
        tokens.add(new Token(SOF, "", null, line));

        while (!isAtEnd()) {
            start = current;
            scanToken();
        }

        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    private void scanToken() {
        char c = consume();
        switch (c) {
            // single-char tokens
            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break;
            case ',': addToken(COMMA); break;
            case '.': addToken(DOT); break;
            case '-': addToken(MINUS); break;
            case '+': addToken(PLUS); break;
            case ';': addToken(SEMICOLON); break;
            case '*': addToken(STAR); break;

            // one or two character tokens
            case '!':
                addToken(consumeIfMatch('=') ? BANG_EQUAL : BANG);
                break;
            case '=':
                addToken(consumeIfMatch('=') ? EQUAL_EQUAL : EQUAL);
                break;
            case '<':
                addToken(consumeIfMatch('=') ? LESS_EQUAL : LESS);
                break;
            case '>':
                addToken(consumeIfMatch('=') ? GREATER_EQUAL : GREATER);
                break;

            case '/':
                if(consumeIfMatch('/')) {
                    consumeSingleLineComment();
                } else if (consumeIfMatch('*')) {
                    consumeMultiLineComment();
                } else {
                    addToken(SLASH);
                }
                break;

            case '"': consumeString();break;

            // skip meaningless characters
            case ' ':
            case '\t':
            case '\r':
                break;

            case '\n':
                line++;
                break;

            default:
                if (isDigit(c)) {    //number
                    consumeNumber();
                } else if(isAlpha(c)) {
                    consumeIdentifier();
                } else {
                    // unexpected token
                    Lox.error(line, "Unexpected character.");
                }
                break;
        }
    }

    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private char peekNext() {
        if (current+1 >= source.length()) return '\0';
        return source.charAt(current+1);
    }

    private char consume() {
        return source.charAt(current++);
    }

    private boolean consumeIfMatch(char expected) {
        if (isAtEnd()) return false;

        if(source.charAt(current) != expected)
            return false;

        current++;
        return true;
    }

    private void consumeSingleLineComment() {
        while(!isAtEnd() && peek() != '\n') consume();
        // On windows, a \r before \n
        String lexeme = source.substring(start, current - 1);
        String literal = source.substring(start + 2, current - 1);
        addToken(SingleLineComment,lexeme,literal);
    }

    private void consumeMultiLineComment() {
        char last = '\0';
        boolean closing = false;
        while(!isAtEnd()) {
            char c = consume();
            if(last == '*' && c == '/') {
                closing = true;
                break;
            }
            last = c;
        }

        if(isAtEnd() && !closing) {
            Lox.error(line, "Unexpected comment.");
            return;
        }

        addToken(MultiLineComment,source.substring(start + 2, current - 2));
    }

    private void consumeString() {
        while(!isAtEnd() && peek() != '"') {
            if(peek() == '\n') line++;
            consume();
        }

        if (isAtEnd()) {
            Lox.error(line, "Unexpected string.");
            return;
        }

        // consume closing quote
        consume();

        // trim surrounding quotes.
        String literal = source.substring(start + 1, current - 1);
        addToken(STRING, literal);

    }

    private void consumeNumber() {
        while(isDigit(peek())) consume();
        if(peek() == '.' && isDigit(peekNext())) {
            do {
                consume();
            } while (isDigit(peek()));
        }
        Double literal = Double.parseDouble(source.substring(start, current));
        addToken(NUMBER, literal);
    }

    private void consumeIdentifier() {
        // According to the maximal match principle.
        // e.g. When keyword can both match "or" and "orchid" , we need choose latter.
        while(isDigit(peek()) || isAlpha(peek())) consume();

        String keyword = source.substring(start, current);
        // If the keyword is a reserved word, select the corresponding token
        TokenType type = keywords.getOrDefault(keyword, IDENTIFIER);
        addToken(type);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        // include start , but exclude till
        String lexeme = source.substring(start, current);
        addToken(type, lexeme, literal);
    }

    private void addToken(TokenType type, String lexeme, Object literal) {
        tokens.add(new Token(type, lexeme, literal, line));
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlpha(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '_';
    }
}
