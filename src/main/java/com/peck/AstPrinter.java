package com.peck;

import com.peck.Expr.Call;

public class AstPrinter implements Expr.Visitor<String> {

    // AstPrinter doesn't case which kind of Expr will be printed
    // it will delegate the choice of method call to Expr itself
    public String print(Expr expr) {
        return expr.accept(this);
    }

    private String parenthesize(String name, Expr... expr) {
        StringBuilder sb = new StringBuilder();

        sb.append("(").append(name);
        for  (Expr e : expr) {
            sb.append(" ");
            sb.append(e.accept(this));
        }
        sb.append(")");

        return sb.toString();
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return parenthesize(expr.operator.getLexeme(), expr.left, expr.right);
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return parenthesize(expr.operator.getLexeme(), expr.right);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if(expr.value == null) return "nil";
        return expr.value.toString();
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return parenthesize("group", expr.expression);
    }

    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        return expr.name.getLexeme();
    }

    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        return expr.value.toString();
    }

    @Override
    public String visitLogicalExpr(Expr.Logical expr) {
        return parenthesize(expr.operator.getLexeme(), expr.left, expr.right);
    }

    @Override
    public String visitCallExpr(Call expr) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitCallExpr'");
    }
}
