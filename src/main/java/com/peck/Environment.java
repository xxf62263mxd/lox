package com.peck;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    private final Map<String, Object> values = new HashMap<>();

    public void define(String name,Object value) {
        values.put(name, value);
    }

    public Object get(Token token) {
        if(values.containsKey(token.getLexeme())) {
            return values.get(token.getLexeme());
        }

        throw new InterpretError(token
                ,"Undefined variable '" + token.getLexeme() + "'.");
    }

    public void assign(Token token, Object value) {
        if(values.containsKey(token.getLexeme())) {
            values.put(token.getLexeme(), value);
            return;
        }
        throw new InterpretError(token
                ,"Undefined variable '" + token.getLexeme() + "'.");
    }
}
