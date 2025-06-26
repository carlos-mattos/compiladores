package lang.builder;

import lang.parser.LangParser;
import lang.parser.LangParserBaseVisitor;
import lang.ast.AstNode;

import java.util.List;
import java.util.stream.Collectors;

public class AstBuilder extends LangParserBaseVisitor<Object> {

    @Override
    public AstNode visitProg(LangParser.ProgContext ctx) {
        List<Object> definitions = ctx.def().stream()
            .map(def -> visit(def))
            .collect(Collectors.toList());
        return new AstNode("prog", "definitions", definitions);
    }

    @Override
    public AstNode visitFunDecl(LangParser.FunDeclContext ctx) {
        String name = ctx.ID().getText();
        List<Object> params = ctx.params() != null ? 
            ctx.params().param().stream()
                .map(param -> visit(param))
                .collect(Collectors.toList()) : 
            List.of();
        List<Object> returnTypes = ctx.typeList() != null ?
            ctx.typeList().type().stream()
                .map(type -> visit(type))
                .collect(Collectors.toList()) :
            List.of();
        Object body = visit(ctx.cmd());
        return new AstNode("funDecl", "name", name, "params", params, "returnTypes", returnTypes, "body", body);
    }

    @Override
    public AstNode visitDataDecl(LangParser.DataDeclContext ctx) {
        String name = ctx.TYID().getText();
        boolean isAbstract = ctx.ABSTRACT() != null;
        
        List<Object> members = new java.util.ArrayList<>();
        
        if (ctx.funDecl() != null) {
            for (var funCtx : ctx.funDecl()) {
                members.add(visit(funCtx));
            }
        }
        
        return new AstNode("dataDecl", "name", name, "isAbstract", isAbstract, "members", members);
    }

    @Override
    public AstNode visitParam(LangParser.ParamContext ctx) {
        String name = ctx.ID().getText();
        Object type = visit(ctx.type());
        return new AstNode("param", "name", name, "type", type);
    }

    @Override
    public AstNode visitSimpleType(LangParser.SimpleTypeContext ctx) {
        return new AstNode("simpleType", "name", ctx.TYID().getText());
    }

    @Override
    public AstNode visitArrayType(LangParser.ArrayTypeContext ctx) {
        Object elementType = visit(ctx.type());
        return new AstNode("arrayType", "elementType", elementType);
    }

    @Override
    public AstNode visitProductType(LangParser.ProductTypeContext ctx) {
        Object left = visit(ctx.type(0));
        Object right = visit(ctx.type(1));
        return new AstNode("productType", "left", left, "right", right);
    }

    @Override
    public AstNode visitBlockCmd(LangParser.BlockCmdContext ctx) {
        List<Object> commands = ctx.block().cmd().stream()
            .map(cmd -> visit(cmd))
            .collect(Collectors.toList());
        return new AstNode("block", "commands", commands);
    }

    @Override
    public AstNode visitIfCmd(LangParser.IfCmdContext ctx) {
        Object condition = visit(ctx.expr());
        Object thenCmd = visit(ctx.cmd(0));
        Object elseCmd = visit(ctx.cmd(1));
        return new AstNode("if", "condition", condition, "then", thenCmd, "else", elseCmd);
    }

    @Override
    public AstNode visitIterateCmd(LangParser.IterateCmdContext ctx) {
        Object condition = visit(ctx.expr());
        Object body = visit(ctx.cmd());
        return new AstNode("iterate", "condition", condition, "body", body);
    }

    @Override
    public AstNode visitPrintCmd(LangParser.PrintCmdContext ctx) {
        Object expr = visit(ctx.expr());
        return new AstNode("print", "expr", expr);
    }

    @Override
    public AstNode visitExprCmd(LangParser.ExprCmdContext ctx) {
        Object expr = visit(ctx.expr());
        return new AstNode("exprCmd", "expr", expr);
    }

    @Override
    public AstNode visitAdd(LangParser.AddContext ctx) {
        Object left = visit(ctx.expr(0));
        Object right = visit(ctx.expr(1));
        String op = ctx.op.getText();
        return new AstNode("bin", "op", op, "left", left, "right", right);
    }

    @Override
    public AstNode visitMult(LangParser.MultContext ctx) {
        Object left = visit(ctx.expr(0));
        Object right = visit(ctx.expr(1));
        String op = ctx.op.getText();
        return new AstNode("bin", "op", op, "left", left, "right", right);
    }

    @Override
    public AstNode visitUnary(LangParser.UnaryContext ctx) {
        String op = ctx.op.getText();
        Object expr = visit(ctx.expr());
        return new AstNode("un", "op", op, "expr", expr);
    }

    @Override
    public AstNode visitRel(LangParser.RelContext ctx) {
        Object left = visit(ctx.expr(0));
        Object right = visit(ctx.expr(1));
        String op = ctx.op.getText();
        return new AstNode("bin", "op", op, "left", left, "right", right);
    }

    @Override
    public AstNode visitBool(LangParser.BoolContext ctx) {
        Object left = visit(ctx.expr(0));
        Object right = visit(ctx.expr(1));
        String op = ctx.op.getText();
        return new AstNode("bin", "op", op, "left", left, "right", right);
    }

    @Override
    public AstNode visitVar(LangParser.VarContext ctx) {
        return new AstNode("var", "name", ctx.ID().getText());
    }

    @Override
    public AstNode visitIntLit(LangParser.IntLitContext ctx) {
        return new AstNode("int", "value", Integer.parseInt(ctx.INT().getText()));
    }

    @Override
    public AstNode visitFloatLit(LangParser.FloatLitContext ctx) {
        return new AstNode("float", "value", Double.parseDouble(ctx.FLOAT().getText()));
    }

    @Override
    public AstNode visitTrueLit(LangParser.TrueLitContext ctx) {
        return new AstNode("bool", "value", true);
    }

    @Override
    public AstNode visitFalseLit(LangParser.FalseLitContext ctx) {
        return new AstNode("bool", "value", false);
    }

    @Override
    public AstNode visitCharLit(LangParser.CharLitContext ctx) {
        String text = ctx.CHAR().getText();
        char value = text.charAt(1);
        if (value == '\\' && text.length() > 2) {
            char escape = text.charAt(2);
            switch (escape) {
                case 'n': value = '\n'; break;
                case 't': value = '\t'; break;
                case 'r': value = '\r'; break;
                default: value = escape; break;
            }
        }
        return new AstNode("char", "value", value);
    }

    @Override
    public AstNode visitParen(LangParser.ParenContext ctx) {
        return (AstNode) visit(ctx.expr());
    }
} 