package com.peck;

public class Interpreter implements Expr.Visitor<Object>{

    public void interpret(Expr expr) {
        try {
            Object val = evaluate(expr);
            System.out.println(stringify(val));
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


    public static class InterpretError extends RuntimeException {
        private final Token token;

        public InterpretError(Token token, String message) {
            super(message);
            this.token = token;
        }

        public Token getToken() {
            return token;
        }
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if(!(operand instanceof Double))
            throw new InterpretError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator, Object operand1,Object operand2) {
        if(!(operand1 instanceof Double && operand2 instanceof Double))
            throw new InterpretError(operator, "Operand must be a number.");
    }
}
