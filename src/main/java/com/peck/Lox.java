package com.peck;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static com.peck.TokenType.EOF;


public class Lox {

    private static final Interpreter interpreter = new Interpreter();

    private static boolean hadError = false;
    private static boolean hadRuntimeError = false;
    private static boolean debug = false;

    public static void main(String[] args) throws IOException {
        args = new String[] {"/Users/peck/code/lox/src/main/resources/func-case.lox"};
        if(args.length > 1) {
            System.out.println("Usage: jox [script]");
            System.exit(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

        if(hadError) System.exit(65);
        if(hadRuntimeError) System.exit(70);
    }

    private static void runPrompt() throws IOException {
        InputStreamReader isr = new InputStreamReader(System.in);
        BufferedReader br = new BufferedReader(isr);

        for(;;){
            System.out.print("> ");
            String line = br.readLine();
            if(line == null) break;
            run(line);

            //If the user makes a mistake, it shouldn't kill the entire session
            hadError = false;
            hadRuntimeError = false;
        }
    }

    private static void run(String source) {
        Scanner sc = new Scanner(source);
        List<Token> tokens = sc.scanTokens();

        if(debug) {
            System.out.println("============== Token ============");
            for (Token token : tokens) {
                System.out.println(token);
            }
        }

        Parser parser = new Parser(tokens);
        List<Stmt> root = parser.parse();

        if(hadError) return;
        Resolver resolver = new Resolver(interpreter);
        resolver.resolve(root);

        if(hadError) return;
        interpreter.interpret(root);

    }

    public static void error(Token token, String message) {
        hadError = true;
        if(token.getType() == EOF) {
            report(token.getLine(),"at end" ,message);
        } else {
            report(token.getLine(),"at '"+ token.getLexeme() +"'" ,message);
        }

    }

    public static void error(int line, String message) {
        hadError = true;
        report(line,"",message);
    }

    public static void runtimeError(InterpretError error) {
        hadRuntimeError = true;
        System.out.println("\033[31m[line " + error.getToken().getLine() + "] " + error.getMessage()+"\033[0m");
    }

    private static void report(int line, String where, String message ) {
        System.out.println("[Line " + line + "] Error " + where + ": " + message);
    }
}