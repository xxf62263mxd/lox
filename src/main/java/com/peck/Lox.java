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
    private static boolean hadError = false;
    private static boolean debug = true;

    public static void main(String[] args) throws IOException {
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
        Expr root = parser.parse();

        if(hadError) return;

        if(debug) {
            System.out.println("============== Expression ============");
            System.out.println(new AstPrinter().print(root));
        }

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

    private static void report(int line, String where, String message ) {
        System.out.println("[Line " + line + "] Error " + where + ": " + message);
    }
}