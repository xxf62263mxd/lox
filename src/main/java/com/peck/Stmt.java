package com.peck;

import java.util.List;

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

    public static class Block extends Stmt {
        final List<Stmt> stmts;

        public Block(List<Stmt> stmts) {
            this.stmts = stmts;
        }
        @Override
        void accept(Visitor visitor) {
            visitor.visitBlockStmt(this);
        }
    }

    public static class If extends Stmt {
        final Expr conditionExpr;
        final Stmt thenStmt;
        final Stmt elseStmt;

        public If(Expr conditionExpr, Stmt thenStmt, Stmt elseStmt) {
            this.conditionExpr = conditionExpr;
            this.thenStmt = thenStmt;
            this.elseStmt = elseStmt;
        }

        @Override
        void accept(Visitor visitor) {
            visitor.visitIfStmt(this);
        }
    }

    public static class While extends Stmt {
        final Expr conditionExpr;
        final Stmt body;

        public While(Expr conditionExpr, Stmt body) {
            this.conditionExpr = conditionExpr;
            this.body = body;
        }

        @Override
        void accept(Visitor visitor) {
            visitor.visitWhileStmt(this);
        }
            
    }

    public static class Function extends Stmt {
        final Token name;
        final List<Token> params;
        final Stmt body;
        
        public Function(Token name, List<Token> params, Stmt body) {
            this.name = name;
            this.params = params;
            this.body = body;
        }

        @Override
        void accept(Visitor visitor) {
            visitor.visitFunctionStmt(this);
        }
    }


    public interface Visitor {
        void visitExpressionStmt(Expression stmt);
        void visitPrintStmt(Print stmt);
        void visitVarDeclaration(VarDeclaration stmt);
        void visitBlockStmt(Block stmt);
        void visitIfStmt(If stmt);
        void visitWhileStmt(While stmt);
        void visitFunctionStmt(Function stmt);
    }
}
