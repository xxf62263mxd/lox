package com.peck;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;


public class Lox {
    private static boolean hadError = false;

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

        for (Token token : tokens) {
            System.out.println(token);
        }
    }

    public static void error(int line, String message) {
        report(line,"",message);
    }

    private static void report(int line, String where, String message ) {
        System.out.println("[Line " + line + "] Error " + where + ": " + message);
        hadError = true;
    }
}