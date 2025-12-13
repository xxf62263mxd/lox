package com.peck;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class Resolver implements Expr.Visitor<Void>, Stmt.Visitor {
    
    private final Interpreter interpreter;
    // 'false' represent this variable only be declare but not be defined, we can't use this variable.
    // 'true' represent this variable is be defined, it is available.
    private final Stack<Map<String, Boolean>> scopes = new Stack<>();
    private FunctionType currentFunction = FunctionType.NONE;

    public Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    // we should resolve the variable be read in some expression(always a variable expression in leaf node in ast).
    // and we should resolve the variable be writed in an assign expression.

    // after resolving, every variable expression (identifer) will have a span that represent 
    // the distance between the current scope and one that variable existed.
    private void doResolve(Expr expr, Token name) {
        int len = scopes.size();
        for(int i = len - 1; i >= 0 ; i--) {
            var scope = scopes.get(i);
            Boolean flag = scope.get(name.getLexeme());
            if(flag != null && flag) {
                interpreter.resolve(expr, len - 1 - i);
                return;
            }
        }
    }

    // we should resolve params and variables in body of function.
    private void resolveFunction(Stmt.Function func, FunctionType type) {
        FunctionType parentType = currentFunction;
        currentFunction = type;
        beginScope();
        for(Token param : func.params) {
            declare(param);
            define(param);
        }
        resolve(func.body.stmts);
        endScope();
        currentFunction = parentType;
    }

    
    private void resolve(Expr expr) {
        expr.accept(this);
    }
    
    private void resolve(Stmt stmt) {
        stmt.accept(this);
    }

    public void resolve(List<Stmt> stmts) {
        for(Stmt stmt: stmts) {
            resolve(stmt);
        }
    }

    // we should push or pop a scope when an environment be create or destory.
    private void beginScope() {
        scopes.push(new HashMap<String,Boolean>());
    }

    private void endScope() {
        scopes.pop();
    }


    // we should declare and define a variable or function when it be created.
    // and we can't use a variable before it be defined (or only be declared).
    private void declare(Token name) {
        if(scopes.isEmpty()) return;
        if(scopes.peek().containsKey(name.getLexeme())) {
            Lox.error(name, "Already a virable with this name in this scope.");
        }
        scopes.peek().put(name.getLexeme(), false);
    }

    private void define(Token name) {
        if(scopes.isEmpty()) return;
        scopes.peek().put(name.getLexeme(), true);
    }


    @Override
    public Void visitBinaryExpr(Expr.Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitUnaryExpr(Expr.Unary expr) {
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitLiteralExpr(Expr.Literal expr) {
        return null;
    }

    @Override
    public Void visitGroupingExpr(Expr.Grouping expr) {
        resolve(expr.expression);
        return null;
    }

    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        if(!scopes.isEmpty() && scopes.peek().get(expr.name.getLexeme()) == Boolean.FALSE) {
            Lox.error(expr.name, "Can't read variable before it be define.");
        }
        doResolve(expr, expr.name);
        return null;
    }

    @Override
    public Void visitAssignExpr(Expr.Assign expr) {
        resolve(expr.value);
        doResolve(expr, expr.name);
        return null;
    }

    @Override
    public Void visitLogicalExpr(Expr.Logical expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitCallExpr(Expr.Call expr) {
        // In the end of subtree, the node still is a variable expression (or a identifier)
        resolve(expr.callee); 
        for(Expr arg: expr.args) {
            resolve(arg);
        }
        return null;
    }

    @Override
    public void visitExpressionStmt(Stmt.Expression stmt) {
        resolve(stmt.expr);
    }

    @Override
    public void visitPrintStmt(Stmt.Print stmt) {
        resolve(stmt.expr);
    }

    @Override
    public void visitVarDeclaration(Stmt.VarDeclaration stmt) {
        declare(stmt.name);
        if (stmt.initializer != null) {
            resolve(stmt.initializer);
        }
        define(stmt.name);
    }

    @Override
    public void visitBlockStmt(Stmt.Block block) {
        beginScope();
        resolve(block.stmts); 
        endScope();
    }

    @Override
    public void visitIfStmt(Stmt.If stmt) {
        resolve(stmt.conditionExpr);
        resolve(stmt.thenStmt);
        if(stmt.elseStmt != null)
            resolve(stmt.elseStmt);
    }

    @Override
    public void visitWhileStmt(Stmt.While stmt) {
        resolve(stmt.conditionExpr);
        resolve(stmt.body);
    }

    @Override
    public void visitFunctionStmt(Stmt.Function stmt) {
        declare(stmt.name);
        define(stmt.name);
        
        resolveFunction(stmt, FunctionType.FUNCTION);
    }

    @Override
    public void visitReturnStmt(Stmt.Return stmt) {
        if(currentFunction == FunctionType.NONE) {
            Lox.error(stmt.keyword, "Can't return from top-level code.");
        }
        if(stmt.value != null)
            resolve(stmt.value);
    }
    
    private enum FunctionType {
        NONE,
        FUNCTION
    }
}
