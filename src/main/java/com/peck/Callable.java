package com.peck;
import java.util.List;

public interface Callable {
    int arity();
    Object call(Interpreter interpreter, List<Object> args);
}
