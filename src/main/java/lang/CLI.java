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
        if (args.length != 2) throw new CLIException("Usage: java -jar lang.jar [-syn|-t|-i|-src|-gen] <file>", 1);

        String flag = args[0], filename = args[1];
        try {
            String input = Files.readString(Path.of(filename));
            ErrorListener err = new ErrorListener();

            LangLexer lexer = new LangLexer(CharStreams.fromString(input));
            lexer.removeErrorListeners(); lexer.addErrorListener(err);
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            LangParser parser = new LangParser(tokens);
            parser.removeErrorListeners(); parser.addErrorListener(err);

            ParseTree tree = parser.prog();

            if (flag.equals("-syn")) {
                System.out.println(err.getSyntaxErrors() == 0 ? "accept" : "reject");
                if (!isTestMode()) System.exit(err.getSyntaxErrors() == 0 ? 0 : 1);
                return;
            }
            if (err.getSyntaxErrors() > 0) throw new CLIException("Syntax errors found!", 1);

            AstBuilder builder = new AstBuilder();
            var ast = (lang.ast.AstNode) builder.visit(tree);

            if (flag.equals("-t") || flag.equals("-src") || flag.equals("-gen")) {
                var diags = new lang.types.Diagnostics();
                var tc = new lang.types.TypeChecker(diags);
                boolean ok = tc.check(ast);

                if (flag.equals("-t")) {
                    System.out.println(ok ? "accept" : "reject");
                    if (!ok) {
                        diags.all().forEach(d -> System.err.println("TYPE: " + d.message()));
                    }
                    if (!isTestMode()) System.exit(ok ? 0 : 1);
                    return;
                }
                if (!ok) {
                    if (isTestMode()) {
                        System.err.println("Typecheck errors:");
                        diags.all().forEach(System.err::println);
                    }
                    throw new CLIException("Typecheck failed", 1);
                }

                if (flag.equals("-src")) {
                    String pretty = new lang.src.PrettyPrinter().print(ast);
                    System.out.println(pretty);
                    return;
                }

                if (flag.equals("-gen")) {
                    String jasmin = new lang.codegen.JasminGen(tc.getTyping(), tc.getFunSigs()).emit(ast);
                    System.out.println(jasmin);
                    return;
                }
            }

            switch (flag) {
                case "-i" -> {
                    Interpreter interpreter = new Interpreter();
                    interpreter.setTestMode(isTestMode());
                    interpreter.interpret(ast);
                }
                default -> throw new CLIException("Usage: java -jar lang.jar [-syn|-t|-i|-src|-gen] <file>", 1);
            }
        } catch (IOException e) {
            throw new CLIException("Error reading file: " + e.getMessage(), 1);
        }
    }
} 