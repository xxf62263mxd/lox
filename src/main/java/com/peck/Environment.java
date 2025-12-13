package com.peck;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    private final Environment parent;
    private final Map<String, Object> values = new HashMap<>();

    public Environment() {
        this.parent = null;
    }

    public Environment(Environment parent) {
        this.parent = parent;
    }

    public void define(String name,Object value) {
        values.put(name, value);
    }

    public Object get(Token token) {
        if(values.containsKey(token.getLexeme())) {
            return values.get(token.getLexeme());
        }

        if(parent != null) {
            return parent.get(token);
        }

        throw new InterpretError(token
                ,"Undefined variable '" + token.getLexeme() + "'.");
    }

    public void assign(Token token, Object value) {
        if(values.containsKey(token.getLexeme())) {
            values.put(token.getLexeme(), value);
            return;
        }

        if(parent != null) {
            parent.assign(token, value);
            return;
        }

        throw new InterpretError(token
                ,"Undefined variable '" + token.getLexeme() + "'.");
    }

    private Environment ancestor(int distance) {
        Environment env = this;
        for (int i = 0; i < distance ; i++) {
            env = env.parent;
        }
        return env;
    }

    public Object getAt(int distance, Token token) {
        return ancestor(distance).get(token);
    }

    public void assignAt(int distance, Token token, Object value) {
        ancestor(distance).assign(token, value);
    }
}
