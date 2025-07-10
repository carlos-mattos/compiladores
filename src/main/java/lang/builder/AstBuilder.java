package lang.builder;

import lang.parser.LangParser;
import lang.parser.LangParserBaseVisitor;
import lang.ast.AstNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;

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
        
        if (ctx.decl() != null) {
            for (var declCtx : ctx.decl()) {
                members.add(visit(declCtx));
            }
        }
        
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
    public AstNode visitPrintCmd(LangParser.PrintCmdContext ctx) {
        Object expr = visit(ctx.expr());
        return new AstNode("print", "expr", expr);
    }

    @Override
    public AstNode visitReadCmd(LangParser.ReadCmdContext ctx) {
        Object lvalue = visit(ctx.lvalue());
        return new AstNode("read", "lvalue", lvalue);
    }

    @Override
    public AstNode visitReturnCmd(LangParser.ReturnCmdContext ctx) {
        List<Object> exprs = ctx.exprList() != null ?
            ctx.exprList().expr().stream()
                .map(expr -> visit(expr))
                .collect(Collectors.toList()) :
            List.of();
        return new AstNode("return", "exprs", exprs);
    }

    @Override
    public AstNode visitAssignCmd(LangParser.AssignCmdContext ctx) {
        Object lvalue = visit(ctx.assign().lvalue());
        Object expr = visit(ctx.assign().expr());
        return new AstNode("assign", "lvalue", lvalue, "expr", expr);
    }

    @Override
    public AstNode visitCallCmd(LangParser.CallCmdContext ctx) {
        String func = ctx.call().ID().getText();
        List<Object> args = ctx.call().exprList() != null ?
            ctx.call().exprList().expr().stream()
                .map(expr -> visit(expr))
                .collect(Collectors.toList()) :
            List.of();
        return new AstNode("call", "func", func, "args", args);
    }

    @Override
    public AstNode visitExprCmd(LangParser.ExprCmdContext ctx) {
        Object expr = visit(ctx.expr());
        return new AstNode("exprCmd", "expr", expr);
    }

    @Override
    public AstNode visitFieldAccess(LangParser.FieldAccessContext ctx) {
        Object obj = visit(ctx.expr());
        String field = ctx.ID().getText();
        return new AstNode("fieldAccess", "obj", obj, "field", field);
    }

    @Override
    public AstNode visitArrayAccess(LangParser.ArrayAccessContext ctx) {
        Object array = visit(ctx.expr(0));
        Object index = visit(ctx.expr(1));
        return new AstNode("arrayAccess", "array", array, "index", index);
    }

    @Override
    public AstNode visitCallExpr(LangParser.CallExprContext ctx) {
        Object func = visit(ctx.expr());
        List<Object> args = ctx.exprList() != null ?
            ctx.exprList().expr().stream()
                .map(expr -> visit(expr))
                .collect(Collectors.toList()) :
            List.of();
        return new AstNode("callExpr", "func", func, "args", args);
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
    public AstNode visitNullLit(LangParser.NullLitContext ctx) {
        return new AstNode("null", "value", null);
    }

    @Override
    public AstNode visitParen(LangParser.ParenContext ctx) {
        Object result = visit(ctx.expr());
        return (result instanceof AstNode) ? (AstNode) result : null;
    }

    @Override
    public AstNode visitIterateCmd(LangParser.IterateCmdContext ctx) {
        Object body = visit(ctx.cmd());
        
        if (ctx.lvalue() != null) {
            // Forma: ITERATE '(' lvalue ':' expr ')' cmd
            Object lvalue = visit(ctx.lvalue());
            Object expr = visit(ctx.expr());
            return new AstNode("iterate", "id", lvalue, "expr", expr, "body", body);
        } else {
            // Forma: ITERATE '(' expr ')' cmd
            Object expr = visit(ctx.expr());
            return new AstNode("iterate", "expr", expr, "body", body);
        }
    }

    @Override
    public AstNode visitCallWithRetCmd(LangParser.CallWithRetCmdContext ctx) {
        Object result = visit(ctx.callWithRet());
        return (result instanceof AstNode) ? (AstNode) result : null;
    }

    public AstNode visitCallWithRet(LangParser.CallWithRetContext ctx) {
        String func = ctx.ID().getText();
        List<Object> args = ctx.exprList() != null ?
            ctx.exprList().expr().stream().map(this::visit).collect(Collectors.toList()) :
            List.of();
        List<Object> lvalues = ctx.lvalue().stream().map(this::visit).collect(Collectors.toList());
        return new AstNode("callWithRet", "func", func, "args", args, "lvalues", lvalues);
    }

    @Override
    public AstNode visitLvalue(LangParser.LvalueContext ctx) {
        AstNode node = new AstNode("lvalue", "name", ctx.ID(0).getText());
        AstNode current = node;
        int exprIdx = 0;
        int idIdx = 1;
        List<TerminalNode> lbracks = ctx.LBRACK();
        List<TerminalNode> dots = ctx.DOT();
        int lbrackIdx = 0, dotIdx = 0;
        for (int i = 1; i < ctx.getChildCount(); i++) {
            if (lbrackIdx < lbracks.size() && ctx.getChild(i).getText().equals("[")) {
                Object index = visit(ctx.expr(exprIdx++));
                AstNode arr = new AstNode("arrayAccess", "index", index);
                current.put("next", arr);
                current = arr;
                lbrackIdx++;
            } else if (dotIdx < dots.size() && ctx.getChild(i).getText().equals(".")) {
                String field = ctx.ID(idIdx++).getText();
                AstNode fld = new AstNode("fieldAccess", "field", field);
                current.put("next", fld);
                current = fld;
                dotIdx++;
                i++; // skip field name
            }
        }
        return node;
    }

    public AstNode visitNewRecord(LangParser.NewRecordContext ctx) {
        return new AstNode("newRec", "type", visit(ctx.type()));
    }

    public AstNode visitNewArray(LangParser.NewArrayContext ctx) {
        return new AstNode("newArr", "type", visit(ctx.type()), "size", visit(ctx.expr()));
    }

    public AstNode visitArrayLit(LangParser.ArrayLitContext ctx) {
        List<Object> elements = ctx.exprList() != null ?
            ctx.exprList().expr().stream().map(this::visit).collect(Collectors.toList()) :
            List.of();
        return new AstNode("arrayLit", "elements", elements);
    }

    public AstNode visitIntType(LangParser.IntTypeContext ctx) {
        return new AstNode("type", "name", "Int");
    }

    public AstNode visitFloatType(LangParser.FloatTypeContext ctx) {
        return new AstNode("type", "name", "Float");
    }

    public AstNode visitBoolType(LangParser.BoolTypeContext ctx) {
        return new AstNode("type", "name", "Bool");
    }

    public AstNode visitCharType(LangParser.CharTypeContext ctx) {
        return new AstNode("type", "name", "Char");
    }

    @Override
    public AstNode visitDecl(LangParser.DeclContext ctx) {
        String name = ctx.ID().getText();
        Object type = visit(ctx.type());
        return new AstNode("decl", "name", name, "type", type);
    }
} 