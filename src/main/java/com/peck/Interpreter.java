package com.peck;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor{

    public Environment globalEnv = new Environment();
    private Environment env =  globalEnv;

    private Map<Expr, Integer> locals = new HashMap<>();

    public void interpret(List<Stmt> stmts) {

        globalEnv.define("clock", new Callable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(List<Object> args) {
                return (double)System.currentTimeMillis() / 1000.0;
            }

            @Override
            public String toString() {
                return "<native fn>";
            }
        });

        try {
            for (Stmt stmt : stmts) {
                execute(stmt);
            }
        } catch (InterpretError e) {
            Lox.runtimeError(e);
        }
    }

    private String stringify(Object val) {
        if (val == null) return "nil";
        if (val instanceof Double d) {
            String text = d.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }
        return val.toString();
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    private void executeBlock(Stmt.Block block, Environment env) {
        Environment parent = this.env;

        //we need catch exception here
        //some keywords,like return, break, continue, all will interrupt this block by thrown an exception
        //we don't catch the exception here, exception will still be thrown up on the stack
        try {
            this.env = env;

            for(Stmt stmt : block.stmts) {
                execute(stmt);
            }

        } finally {
            this.env = parent;
        }
    }

    private Object evaluate(Expr expr){
        return expr.accept(this);
    }

    private boolean isTruthy(Object value){
        if(value == null) return false;
        if(value instanceof Boolean b) return b;
        return true;
    }

    private boolean isEqual(Object a, Object b){
        if(a == null && b == null) return true;
        if(a == null) return false;
        return a.equals(b);
    }

    protected void resolve(Expr expr, int span) {
        locals.put(expr, span);
    }

    private Object lookUpVariable(Expr expr, Token name) {
        Integer distance = locals.get(expr);
        if(distance != null) {
            return env.getAt(distance, name);
        } else {
            return globalEnv.get(name);
        }
    }

    private void assignVariable(Expr expr, Token name, Object value) {
        Integer distance = locals.get(expr);
        if(distance != null) {
            env.assignAt(distance, name, value);
        } else {
            globalEnv.assign(name, value);
        }
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.getType()) {

            case PLUS:
                if (left instanceof Double && right instanceof Double)
                    return (double)left + (double)right;
                if (left instanceof String && right instanceof String)
                    return (String)left + (String)right;
                throw new InterpretError(expr.operator
                        , "All operand must be either numbers or strings.");
            case MINUS:
                checkNumberOperands(expr.operator,left,right);
                return (double)left - (double)right;
            case SLASH:
                checkNumberOperands(expr.operator,left,right);
                if ((double)right == 0)
                    throw new InterpretError(expr.operator, "The divisor cannot be zero.");
                return (double)left / (double)right;
            case STAR:
                checkNumberOperands(expr.operator,left,right);
                return (double)left * (double)right;



            case GREATER:
                checkNumberOperands(expr.operator,left,right);
                return (double)left > (double)right;
            case LESS:
                checkNumberOperands(expr.operator,left,right);
                return (double)left < (double)right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator,left,right);
                return (double)left >= (double)right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator,left,right);
                return (double)left <= (double)right;



            case EQUAL_EQUAL:
                return isEqual(left, right);
            case BANG_EQUAL:
                return !isEqual(left, right);

            default:
                throw new InterpretError(expr.operator, "Unexpected operator.");
        }
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.getType()) {
            case MINUS:
                checkNumberOperand(expr.operator,right);
                return -(double) right;
            case BANG:
                return !isTruthy(right);
            default:
                throw new InterpretError(expr.operator, "Unexpected operator.");
        }
    }



    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return lookUpVariable(expr, expr.name);
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object val = evaluate(expr.value);
        assignVariable(expr, expr.name, val);
        return val;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        if(expr.operator.getType() == TokenType.OR) {
            if(isTruthy(left)) return left;
        } else {
            if(!isTruthy(left)) return left;
        }

        return evaluate(expr.right);
    }

    @Override
    public Object visitGetExpr(Expr.Get expr) {
        Object obj = evaluate(expr.obj);

        if(obj instanceof Instance ins) {
            return ins.get(expr.name);
        }

        throw new InterpretError(expr.name, "Only instances have properties.");
    }

    @Override
    public Object visitSetExpr(Expr.Set expr) {
        Object obj = evaluate(expr.obj);
        

        if(obj instanceof Instance ins) {
            Object val = evaluate(expr.value);    
            ins.set(expr.name, val);
            return val;
        }

        throw new InterpretError(expr.name, "Only instances have fields.");
    }

    @Override
    public Object visitThisExpr(Expr.This expr) {
        return lookUpVariable(expr, expr.token);
    }

    @Override
    public void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expr);
    }

    @Override
    public void visitPrintStmt(Stmt.Print stmt) {
        Object val = evaluate(stmt.expr);
        System.out.println(stringify(val));
    }

    @Override
    public void visitVarDeclaration(Stmt.VarDeclaration stmt) {
        Object val = null;
        if(stmt.initializer != null) {
            val =  evaluate(stmt.initializer);
        }
        env.define(stmt.name.getLexeme(), val);
    }

    @Override
    public void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt, new Environment(this.env));
    }

    @Override
    public void visitIfStmt(Stmt.If stmt) {
        if(isTruthy(evaluate(stmt.conditionExpr))) {
            execute(stmt.thenStmt);
        }else if(stmt.elseStmt != null) {
            execute(stmt.elseStmt);
        }
    }

    @Override
    public void visitWhileStmt(Stmt.While stmt) {
        while(isTruthy(evaluate(stmt.conditionExpr))) {
            execute(stmt.body);
        }
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);

        List<Object> args = new ArrayList<>();
        for(Expr argExpr : expr.args) {
            args.add(evaluate(argExpr));
        }

        if(!(callee instanceof Callable)) {
            throw new InterpretError(expr.paren, "Can only call functions and classes.");
        }

        Callable func = (Callable) callee;
        if(func.arity() != expr.args.size()) {
            throw new InterpretError(expr.paren, "Expected " +
                func.arity() + " arguments but got " +
                args.size() + ".");
        }

        return func.call(args);
    }

    @Override
    public void visitFunctionStmt(Stmt.Function stmt) {
        Function func = new Function(stmt, env, false);
        env.define(stmt.name.getLexeme(), func);
    }

    @Override
    public void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if(stmt.value != null)
            value = evaluate(stmt.value);

        throw new ReturnValue(stmt.keyword, value);
    }

    @Override
    public void visitClassStmt(Stmt.Class stmt) {
        env.define(stmt.name.getLexeme(), null);
        Map<String, Function> methods = new HashMap<>();
        for(Stmt.Function method : stmt.methods) {
            methods.put(method.name.getLexeme()
                , new Function(method, env
                    , method.name.getLexeme().equals("init")));
        }

        Class cls = new Class(stmt, methods);
        env.assign(stmt.name, cls);
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if(!(operand instanceof Double))
            throw new InterpretError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator, Object operand1,Object operand2) {
        if(!(operand1 instanceof Double && operand2 instanceof Double))
            throw new InterpretError(operator, "Operand must be a number.");
    }

    /**
     * a Callable can be a function or a class construction
     */
    interface Callable {
        int arity();
        Object call(List<Object> args);
    }

    /** 
     * function call = function template code + independent environment for execution .
     * This class wraps function code and provides a locally-function environment
     */
    private class Function implements Callable{

        final Stmt.Function func;
        final Environment closure;

        final boolean isInitiallizer;

        public Function(Stmt.Function code, Environment closure, boolean isInitiallizer) {
            this.func = code;
            this.closure = closure;
            this.isInitiallizer = isInitiallizer;
        }

        @Override
        public int arity() {
            return func.params.size();
        }

        @Override
        public Object call(List<Object> args) {
            Environment env = new Environment(closure);
            
            //bind params into env
            for(int i = 0; i < arity() ; i++) {
                String name = func.params.get(i).getLexeme();
                Object value = args.get(i);
                env.define(name, value);
            }

            try {
                executeBlock(func.body, env);
            } catch(ReturnValue r) {
                if(isInitiallizer) {
                    return closure.getAt(0, r.token, "this");
                }
                return r.value;
            }
            
            return null;
        }

        public Function bind(Instance ins) {
            Environment bound = new Environment(closure);
            bound.define("this", ins);
            return new Function(func, bound, isInitiallizer);
        } 

        @Override
        public String toString() {
            return "<fn " + func.name + ">";
        }
        
    }

    //we disguise an RuntimeException as ReturnValue to interrupt java stack
    private class ReturnValue extends RuntimeException{
        final Token token;
        final Object value;

        public ReturnValue(Token token, Object value) {
            super(null, null, false, false);
            this.token = token;
            this.value = value;
        }
    }

    private class Class implements Callable {

        final Stmt.Class stmt;
        final Map<String, Function> methods;

        public Class(Stmt.Class stmt, Map<String, Function> methods) {
            this.stmt = stmt;
            this.methods = methods;
        }

        public Function findMethod(String name) {
            return methods.get(name);
        }

        @Override
        public int arity() {
            Function init = findMethod("init");
            if(init != null) {
                return init.arity();
            }
            return 0;
        }

        @Override
        public Object call(List<Object> args) {
            Instance ins = new Instance(this);
            Function init = findMethod("init");
            if(init != null) {
                init.bind(ins).call(args);
            }

            return ins;
        }
        
        @Override
        public String toString() {
            return "<class " + stmt.name.getLexeme() + ">"; 
        }
        
    }

    private class Instance {
        final Class cls;
        private Map<String, Object> fields = new HashMap<>();

        public Instance(Class cls) {
            this.cls = cls;
        }

        public Object get(Token name) {
            if(fields.containsKey(name.getLexeme())) {
                return fields.get(name.getLexeme()); 
            }

            Function method = cls.findMethod(name.getLexeme());
            if(method != null) return method.bind(this); 

            throw new InterpretError(name, "Undefined property '" + name.getLexeme() + "'.");
        }

        public void set(Token name, Object val) {
            fields.put(name.getLexeme(), val);
        }


        @Override
        public String toString() {
            return "<instance " + cls.stmt.name.getLexeme() + ">";  
        }
    }
}
