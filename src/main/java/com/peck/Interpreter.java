package com.peck;

import java.util.ArrayList;
import java.util.List;

import com.peck.Expr.Call;
import com.peck.Stmt.While;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor{

    public Environment globalEnv = new Environment();
    private Environment env =  globalEnv;

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
        return env.get(expr.name);
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object val = evaluate(expr.value);
        env.assign(expr.name, val);
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
    public void visitWhileStmt(While stmt) {
        while(isTruthy(evaluate(stmt.conditionExpr))) {
            execute(stmt.body);
        }
    }

    @Override
    public Object visitCallExpr(Call expr) {
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
        Function func = new Function(stmt);
        env.define(stmt.name.getLexeme(), func);
    }

    @Override
    public void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if(stmt.value != null)
            value = evaluate(stmt.value);

        throw new ReturnValue(value);
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

        public Function(Stmt.Function code) {
            this.func = code;
        }

        @Override
        public int arity() {
            return func.params.size();
        }

        @Override
        public Object call(List<Object> args) {
            Environment env = new Environment(globalEnv);
            
            //bind params into env
            for(int i = 0; i < arity() ; i++) {
                String name = func.params.get(i).getLexeme();
                Object value = args.get(i);
                env.define(name, value);
            }

            try {
                executeBlock(func.body, env);
            } catch(ReturnValue r) {
                return r.value;
            }
            
            return null;
        }

        @Override
        public String toString() {
            return "<fn " + func.name + ">";
        }
        
    }

    //we disguise an RuntimeException as ReturnValue to interrupt java stack
    private class ReturnValue extends RuntimeException{
        final Object value;

        public ReturnValue(Object value) {
            super(null, null, false, false);
            this.value = value;
        }
    }
}
