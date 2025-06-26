package lang;

import lang.parser.*;
import lang.builder.AstBuilder;
import lang.ast.AstNode;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.FileInputStream;
import java.nio.file.Path;

public class CLI {
    public static void main(String[] args) throws Exception {
        if (args.length != 2 || (!args[0].equals("-syn") && !args[0].equals("-i"))) {
            System.err.println("Uso: java -jar lang.jar [-syn|-i] arquivo.lang");
            System.exit(1);
        }

        Path file = Path.of(args[1]);
        try (FileInputStream in = new FileInputStream(file.toFile())) {

            CharStream input = CharStreams.fromStream(in);
            LangLexer lexer = new LangLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            LangParser parser = new LangParser(tokens);

            ParseTree tree = parser.prog();

            if (args[0].equals("-syn")) {
                if (parser.getNumberOfSyntaxErrors() == 0) System.out.println("accept");
                else System.out.println("reject");
            } else { // -i
                if (parser.getNumberOfSyntaxErrors() > 0) {
                    System.err.println("Programa inválido.");
                    System.exit(2);
                }
                
                // Construir a AST
                AstBuilder builder = new AstBuilder();
                AstNode ast = (AstNode) builder.visit(tree);
                
                // TODO: interpretar a AST
                System.err.println("(interpretador ainda não implementado)");
                System.err.println("AST construída: " + ast.get("type"));
                if (ast.get("definitions") instanceof java.util.List) {
                    System.err.println("Definições: " + ((java.util.List<?>) ast.get("definitions")).size());
                }
            }
        }
    }
} 