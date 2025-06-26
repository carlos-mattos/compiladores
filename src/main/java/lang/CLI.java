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
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java -jar lang.jar [-syn|-i] <file>");
            System.exit(1);
        }

        String flag = args[0];
        String filename = args[1];

        if (!flag.equals("-syn") && !flag.equals("-i")) {
            System.err.println("Usage: java -jar lang.jar [-syn|-i] <file>");
            System.exit(1);
        }

        try {
            String input = Files.readString(Path.of(filename));
            
            LangLexer lexer = new LangLexer(CharStreams.fromString(input));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            LangParser parser = new LangParser(tokens);
            
            ParseTree tree = parser.prog();
            
            if (flag.equals("-syn")) {
                if (parser.getNumberOfSyntaxErrors() == 0) {
                    System.out.println("accept");
                } else {
                    System.out.println("reject");
                }
                return;
            }
            
            if (parser.getNumberOfSyntaxErrors() > 0) {
                System.err.println("Syntax errors found!");
                System.exit(1);
            }
            
            AstBuilder builder = new AstBuilder();
            var ast = (lang.ast.AstNode) builder.visit(tree);
            
            Interpreter interpreter = new Interpreter();
            interpreter.interpret(ast);
            
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            System.exit(1);
        }
    }
} 