package com.peck;

public abstract class Stmt {

    abstract void accept(Visitor visitor);

    public static class Expression extends Stmt {
        final Expr expr;

        public Expression(Expr expr) {
            this.expr = expr;
        }

        @Override
        void accept(Visitor visitor) {
            visitor.visitExpressionStmt(this);
        }
    }

    public static class Print extends Stmt {
        final Expr expr;

        public Print(Expr expr) {
            this.expr = expr;
        }

        @Override
        void accept(Visitor visitor) {
            visitor.visitPrintStmt(this);
        }
    }
    
    public interface Visitor {
        void visitExpressionStmt(Expression stmt);
        void visitPrintStmt(Print stmt);
    }
}
