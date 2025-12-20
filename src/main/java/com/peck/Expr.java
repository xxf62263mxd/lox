package com.peck;

import java.util.List;

public abstract class Expr {


    // Expr doesn't case how it is used by Visitor,
    // it only case Visitor has a corresponding method to use itself.
    abstract <R> R accept(Visitor<R> visitor);

    // Compare with Binary, Logical expression will short-circuit
    public static class Logical extends Expr {
        final Expr left;
        final Token operator;
        final  Expr right;
        public Logical(Expr left, Token operator, Expr right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitLogicalExpr(this);
        }
    }

    public static class Binary extends Expr {
        final Expr left;
        final Token operator;
        final Expr right;
        public Binary(Expr left, Token operator, Expr right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }


        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitBinaryExpr(this);
        }
    }

    public static class Unary extends Expr {
        final Token operator;
        final Expr right;
        public Unary(Token operator, Expr right) {
            this.operator = operator;
            this.right = right;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitUnaryExpr(this);
        }
    }

    public static class Literal extends Expr {
        final Object value;
        public Literal(Object value) {
            this.value = value;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitLiteralExpr(this);
        }
    }

    public static class Grouping extends Expr {
        final Expr expression;
        public Grouping(Expr expression) {
            this.expression = expression;
        }


        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitGroupingExpr(this);
        }
    }

    public static class Variable extends Expr {
        final Token name;

        public Variable(Token name) {
            this.name = name;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitVariableExpr(this);
        }
    }

    public static class Assign extends Expr {
        final Token name;
        final Expr value;

        public Assign(Token name, Expr value) {
            this.name = name;
            this.value = value;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitAssignExpr(this);
        }
    }

    public static class Call extends Expr {
        final Expr callee;
        final Token paren;
        final List<Expr> args;
        
        public Call(Expr callee, Token paren, List<Expr> args) {
            this.callee = callee;
            this.paren = paren;
            this.args = args;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitCallExpr(this);
        }
    }

    public static class Get extends Expr {
        final Expr obj;
        final Token name;

        public Get(Expr obj, Token name) {
            this.obj = obj;
            this.name = name;
        }
        
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitGetExpr(this);
        }
    
    }

    public static class Set extends Expr {
        final Expr obj;
        final Token name;
        final Expr value;

        public Set(Expr obj, Token name, Expr value) {
            this.obj = obj;
            this.name = name;
            this. value = value;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitSetExpr(this);
        }
    }

    public static class This extends Expr {
        final Token token;
        public This(Token token) {
            this.token = token;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitThisExpr(this);
        }
    }


    // A Visitor can be regarded as an operator for each Expr
    // This is an appropriate pattern design when we have a pure data class
    // and have not yet which operations will be applied to that class.
    public interface Visitor<R> {
        R visitBinaryExpr(Binary expr);
        R visitUnaryExpr(Unary expr);
        R visitLiteralExpr(Literal expr);
        R visitGroupingExpr(Grouping expr);
        R visitVariableExpr(Variable expr);
        R visitAssignExpr(Assign expr);
        R visitLogicalExpr(Logical expr);
        R visitCallExpr(Call expr);
        R visitGetExpr(Get expr);
        R visitSetExpr(Set expr);
        R visitThisExpr(This expr);
    }
}
