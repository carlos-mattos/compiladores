/*
 * allan amaral - 201935001
 * carlos mattos - 201935003
 */

package lang;

import lang.parser.LangLexer;
import lang.parser.LangParser;
import lang.builder.AstBuilder;
import lang.interpreter.Interpreter;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CLI {
    public static class CLIException extends RuntimeException {
        public final int exitCode;
        public CLIException(String message, int exitCode) {
            super(message);
            this.exitCode = exitCode;
        }
    }
    
    public static void main(String[] args) {
        try {
            run(args);
        } catch (CLIException e) {
            System.err.println(e.getMessage());
            System.exit(e.exitCode);
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            System.exit(1);
        }
    }
    
    private static boolean testMode = false;
    
    public static void setTestMode(boolean mode) {
        testMode = mode;
    }
    
    private static boolean isTestMode() {
        return testMode;
    }
    
    public static void run(String[] args) {
        if (args.length != 2) {
            throw new CLIException("Usage: java -jar lang.jar [-syn|-i] <file>", 1);
        }

        String flag = args[0];
        String filename = args[1];

        if (!flag.equals("-syn") && !flag.equals("-i")) {
            throw new CLIException("Usage: java -jar lang.jar [-syn|-i] <file>", 1);
        }

        try {
            String input = Files.readString(Path.of(filename));
            
            ErrorListener errorListener = new ErrorListener();
            
            LangLexer lexer = new LangLexer(CharStreams.fromString(input));
            lexer.removeErrorListeners();
            lexer.addErrorListener(errorListener);
            
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            LangParser parser = new LangParser(tokens);
            parser.removeErrorListeners();
            parser.addErrorListener(errorListener);
            
            ParseTree tree = parser.prog();
            
            if (flag.equals("-syn")) {
                if (errorListener.getSyntaxErrors() == 0) {
                    System.out.println("accept");
                    if (!isTestMode()) {
                        System.exit(0);
                    }
                } else {
                    System.out.println("reject");
                    if (!isTestMode()) {
                        System.exit(1);
                    }
                }
                return;
            }
            
            if (errorListener.getSyntaxErrors() > 0) {
                throw new CLIException("Syntax errors found!", 1);
            }
            
            AstBuilder builder = new AstBuilder();
            var ast = (lang.ast.AstNode) builder.visit(tree);
            
            Interpreter interpreter = new Interpreter();
            interpreter.setTestMode(isTestMode());
            interpreter.interpret(ast);
            
        } catch (IOException e) {
            throw new CLIException("Error reading file: " + e.getMessage(), 1);
        }
    }
} 