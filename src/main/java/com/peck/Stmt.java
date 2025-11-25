package com.peck;

public abstract class Stmt {

    abstract void accept(Visitor visitor);

    public static class VarDeclaration extends Stmt {
        final Token name;
        final Expr initializer;

        public VarDeclaration(Token name, Expr initializer) {
            this.name = name;
            this.initializer = initializer;
        }

        @Override
        void accept(Visitor visitor) {
            visitor.visitVarDeclaration(this);
        }
    }

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
        void visitVarDeclaration(VarDeclaration stmt);
    }
}
