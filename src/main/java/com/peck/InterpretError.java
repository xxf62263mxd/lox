package com.peck;

public class InterpretError extends RuntimeException {
    private final Token token;

    public InterpretError(Token token, String message) {
        super(message);
        this.token = token;
    }

    public Token getToken() {
        return token;
    }
}
