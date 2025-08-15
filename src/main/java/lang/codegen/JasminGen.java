/*
 * allan amaral - 201935001
 * carlos mattos - 201935003
 */

package lang.codegen;

import lang.ast.AstNode;
import lang.types.Type;
import lang.types.Symbols;
import java.util.*;

public class JasminGen {
  private final StringBuilder j = new StringBuilder();
  private int labelCounter = 0;
  private int localCounter = 0;
  private final Map<String, Integer> localVars = new HashMap<>();
  private final Deque<Map<String,Integer>> scopeStack = new ArrayDeque<>();

  private enum JvmKind { I, F, A }
  private final Map<String, JvmKind> localKinds = new HashMap<>();

  private final Map<AstNode, Type> typing;
  private final Map<String, Symbols.FunSig> funSigs;

  public JasminGen(){ this.typing = Map.of(); this.funSigs = Map.of(); }
  public JasminGen(Map<AstNode, Type> typing, Map<String, Symbols.FunSig> funSigs){
    this.typing = typing == null ? Map.of() : typing;
    this.funSigs = funSigs == null ? Map.of() : funSigs;
  }

  private void emitLoad(String name) {
    int slot = getLocalVar(name);
    JvmKind k = localKinds.getOrDefault(name, JvmKind.I);
    switch (k) {
      case I -> j.append("  iload ").append(slot).append("\n");
      case F -> j.append("  fload ").append(slot).append("\n");
      case A -> j.append("  aload ").append(slot).append("\n");
    }
  }

  private void emitStore(String name, JvmKind k) {
    int slot = getLocalVar(name);
    localKinds.put(name, k);
    switch (k) {
      case I -> j.append("  istore ").append(slot).append("\n");
      case F -> j.append("  fstore ").append(slot).append("\n");
      case A -> j.append("  astore ").append(slot).append("\n");
    }
  }

  private JvmKind kindOfExpr(AstNode e) {
    if (e == null) return JvmKind.A;
    Type t = typing.get(e);
    if (t == null) {
      String k = (String) e.get("type");
      if ("var".equals(k)) return localKinds.getOrDefault((String)e.get("name"), JvmKind.I);
      return JvmKind.I;
    }
    if (t instanceof Type.Prim p) {
      return (p==Type.Prim.FLOAT) ? JvmKind.F : JvmKind.I;
    }
    return JvmKind.A;
  }

  private String jdesc(Type t){
    if (t instanceof Type.Prim p) return switch (p){
      case INT -> "I"; case FLOAT -> "F"; case BOOL -> "Z"; case CHAR -> "C"; };
    if (t instanceof Type.StringT) return "Ljava/lang/String;";
    if (t instanceof Type.NullT) return "Ljava/lang/Object;";
    if (t instanceof Type.Array a) return "["+jdesc(a.elem);
    if (t instanceof Type.Product) return "[Ljava/lang/Object;";
    if (t instanceof Type.Record) return "Ljava/util/HashMap;";
    return "Ljava/lang/Object;";
  }

  private String printlnDesc(Type t){
    if (t instanceof Type.Prim p) return switch (p){
      case INT -> "(I)V"; case FLOAT -> "(F)V"; case BOOL -> "(Z)V"; case CHAR -> "(C)V"; };
    if (t instanceof Type.StringT) return "(Ljava/lang/String;)V";
    return "(Ljava/lang/Object;)V";
  }
  
  public String emit(AstNode ast) {
    j.append(".class public LangMain\n");
    j.append(".super java/lang/Object\n\n");

    j.append(".method public <init>()V\n");
    j.append("  .limit stack 1\n");
    j.append("  .limit locals 1\n");
    j.append("  aload_0\n");
    j.append("  invokespecial java/lang/Object/<init>()V\n");
    j.append("  return\n");
    j.append(".end method\n\n");

    if ("prog".equals(ast.get("type"))) {
      @SuppressWarnings("unchecked")
      List<Object> defs = (List<Object>) ast.get("definitions");
      if (defs != null) {
        for (Object def : defs) {
          if (!(def instanceof AstNode)) continue;
          AstNode node = (AstNode) def;
          String nt = (String) node.get("type");
          if ("funDecl".equals(nt)) {
            emitFunction(node);
          } else if ("dataDecl".equals(nt)) {
            @SuppressWarnings("unchecked")
            List<Object> members = (List<Object>) node.get("members");
            if (members != null) {
              for (Object m : members) {
                if (m instanceof AstNode fn && "funDecl".equals(fn.get("type"))) {
                  emitFunction(fn);
                }
              }
            }
          }
        }
      }

      j.append(".method public static main([Ljava/lang/String;)V\n");
      j.append("  .limit stack 4\n");
      j.append("  .limit locals 1\n");
      j.append("  invokestatic LangMain/main()V\n");
      j.append("  return\n");
      j.append(".end method\n");
    }

    return j.toString();
  }
  
  private void emitFunction(AstNode func) {
    String name = (String) func.get("name");
    localVars.clear(); localKinds.clear(); localCounter = 0;

    j.append(".method public static ").append(name).append("(");
    
    @SuppressWarnings("unchecked")
    List<Object> params = (List<Object>) func.get("params");
    if (params != null) {
      for (int i = 0; i < params.size(); i++) {
        AstNode p = (AstNode) params.get(i);
        Type pt = null;
        if (funSigs.containsKey(name)) pt = funSigs.get(name).params.get(i);
        j.append(pt==null?"I":jdesc(pt));
      }
    }
    
    j.append(")");
    
    @SuppressWarnings("unchecked")
    List<Object> returnTypes = (List<Object>) func.get("returnTypes");
    if (returnTypes != null && !returnTypes.isEmpty()) {
      if (returnTypes.size() == 1) {
        Type rt = null;
        if (funSigs.containsKey(name)) rt = funSigs.get(name).returns.get(0);
        j.append(rt==null?"I":jdesc(rt));
      } else {
        j.append("[Ljava/lang/Object;");
      }
    } else {
      j.append("V");
    }
    
    j.append("\n");
    j.append("  .limit stack 128\n");
    j.append("  .limit locals 128\n\n");

    if (params != null) {
      for (int i = 0; i < params.size(); i++) {
        AstNode param = (AstNode) params.get(i);
        String paramName = (String) param.get("name");
        localVars.put(paramName, i);
        Type pt = funSigs.getOrDefault(name, new Symbols.FunSig(List.of(), List.of())).params.size()>i ? funSigs.get(name).params.get(i) : null;
        localKinds.put(paramName, kindFromType(pt));
        localCounter = Math.max(localCounter, i);
      }
    }
    
    if (func.get("body") != null) emitCommand((AstNode) func.get("body"));
    
    if (returnTypes == null || returnTypes.isEmpty()) {
      j.append("  return\n");
    } else if (returnTypes.size() == 1) {
      j.append("  iconst_0\n");
      j.append("  ireturn\n");
    } else {
      j.append("  aconst_null\n");
      j.append("  areturn\n");
    }
    
    j.append(".end method\n\n");
  }
  
  private void emitCommand(AstNode cmd) {
    if (cmd == null) return;
    
    switch ((String) cmd.get("type")) {
      case "block" -> {
        scopePush();
        @SuppressWarnings("unchecked")
        List<Object> commands = (List<Object>) cmd.get("commands");
        if (commands != null) {
          for (Object command : commands) {
            if (command instanceof AstNode node) {
              emitCommand(node);
            }
          }
        }
        scopePop();
      }
      case "if" -> {
        emitExpression((AstNode) cmd.get("condition"));
        
        String elseLabel = "L" + (++labelCounter);
        String endLabel = "L" + (++labelCounter);
        
        j.append("  ifeq ").append(elseLabel).append("\n");
        
        if (cmd.get("then") != null) {
          emitCommand((AstNode) cmd.get("then"));
        }
        
        j.append("  goto ").append(endLabel).append("\n");
        
        j.append(elseLabel).append(":\n");
        if (cmd.get("else") != null) {
          emitCommand((AstNode) cmd.get("else"));
        }
        
        j.append(endLabel).append(":\n");
      }
      case "iterate" -> {
        if (cmd.get("id") != null) {
          String id = extractIdName(cmd.get("id"));
          AstNode arrExpr = (AstNode) cmd.get("expr");
          emitExpression(arrExpr);
          Type exprT = typing.get(arrExpr);
          Type elemT = (exprT instanceof Type.Array a) ? a.elem : Type.Prim.INT;
          JvmKind ek = kindFromType(elemT);

          int arrSlot = getLocalVar("__arr" + id);
          j.append("  astore ").append(arrSlot).append("\n");

          int iSlot = getLocalVar("__i" + id);
          j.append("  iconst_0\n");
          j.append("  istore ").append(iSlot).append("\n");

          String loopLabel = "L" + (++labelCounter);
          String endLabel = "L" + (++labelCounter);

          j.append(loopLabel).append(":\n");
          j.append("  iload ").append(iSlot).append("\n");
          j.append("  aload ").append(arrSlot).append("\n");
          j.append("  arraylength\n");
          j.append("  if_icmpge ").append(endLabel).append("\n");

          j.append("  aload ").append(arrSlot).append("\n");
          j.append("  iload ").append(iSlot).append("\n");
          if (ek == JvmKind.F) j.append("  faload\n");
          else if (ek == JvmKind.A) j.append("  aaload\n");
          else j.append("  iaload\n");

          emitStore(id, ek);

          if (cmd.get("body") != null) emitCommand((AstNode) cmd.get("body"));

          j.append("  iinc ").append(iSlot).append(" 1\n");
          j.append("  goto ").append(loopLabel).append("\n");
          j.append(endLabel).append(":\n");
        } else {
          emitExpression((AstNode) cmd.get("expr"));
          
          String loopLabel = "L" + (++labelCounter);
          String endLabel = "L" + (++labelCounter);
          
          j.append("  istore ").append(getLocalVar("__count")).append("\n");
          j.append("  iconst_0\n");
          j.append("  istore ").append(getLocalVar("__i")).append("\n");
          
          j.append(loopLabel).append(":\n");
          j.append("  iload ").append(getLocalVar("__i")).append("\n");
          j.append("  iload ").append(getLocalVar("__count")).append("\n");
          j.append("  if_icmpge ").append(endLabel).append("\n");
          
          if (cmd.get("body") != null) {
            emitCommand((AstNode) cmd.get("body"));
          }
          
          j.append("  iinc ").append(getLocalVar("__i")).append(" 1\n");
          j.append("  goto ").append(loopLabel).append("\n");
          
          j.append(endLabel).append(":\n");
        }
      }
      case "print" -> {
        j.append("  getstatic java/lang/System/out Ljava/io/PrintStream;\n");
        AstNode e = (AstNode) cmd.get("expr");
        emitExpression(e);
        Type te = typing.get(e);
        j.append("  invokevirtual java/io/PrintStream/println").append(printlnDesc(te==null? Type.NullT.INSTANCE : te)).append("\n");
      }
      case "read" -> {
        String varName = (String) ((AstNode) cmd.get("lvalue")).get("name");
        j.append("  new java/util/Scanner\n");
        j.append("  dup\n");
        j.append("  getstatic java/lang/System/in Ljava/io/InputStream;\n");
        j.append("  invokespecial java/util/Scanner/<init>(Ljava/io/InputStream;)V\n");
        j.append("  invokevirtual java/util/Scanner/nextInt()I\n");
        j.append("  istore ").append(getLocalVar(varName)).append("\n");
      }
      case "return" -> {
        @SuppressWarnings("unchecked")
        List<Object> exprs = (List<Object>) cmd.get("exprs");
        if (exprs != null && !exprs.isEmpty()) {
          if (exprs.size() == 1) {
            AstNode e = (AstNode) exprs.get(0);
            emitExpression(e);
            Type et = typing.get(e);
            if (et instanceof Type.Prim p && p==Type.Prim.FLOAT) j.append("  freturn\n");
            else if (et instanceof Type.Prim) j.append("  ireturn\n");
            else j.append("  areturn\n");
          } else {
            j.append("  iconst_").append(exprs.size()).append("\n");
            j.append("  anewarray java/lang/Object\n");
            for (int i = 0; i < exprs.size(); i++) {
              j.append("  dup\n");
              emitIntConst(i);
              AstNode e = (AstNode) exprs.get(i);
              emitExpression(e);
              boxTop(typing.get(e));
              j.append("  aastore\n");
            }
            j.append("  areturn\n");
          }
        } else {
          j.append("  return\n");
        }
      }
      case "assign" -> {
        AstNode lv = (AstNode) cmd.get("lvalue");
        AstNode last = lastStep(lv);
        if (last == null) {
          AstNode rhs = (AstNode) cmd.get("expr");
          emitExpression(rhs);
          emitStore((String) lv.get("name"), kindOfExpr(rhs));
        } else if ("arrayAccess".equals(last.get("type"))) {
          String base = (String) lv.get("name");
          j.append("  aload ").append(getLocalVar(base)).append("\n");
          emitExpression((AstNode) last.get("index"));
          AstNode rhs = (AstNode) cmd.get("expr");
          emitExpression(rhs);
          Type arrT = typing.get((AstNode) ((AstNode) lv).get("type"));
          JvmKind rk = kindOfExpr(rhs);
          if (rk==JvmKind.F) j.append("  fastore\n");
          else if (rk==JvmKind.A) j.append("  aastore\n");
          else j.append("  iastore\n");
        } else if ("fieldAccess".equals(last.get("type"))) {
          String base = (String) lv.get("name");
          j.append("  aload ").append(getLocalVar(base)).append("\n");
          j.append("  ldc \"").append(last.get("field")).append("\"\n");
          AstNode rhs = (AstNode) cmd.get("expr");
          emitExpression(rhs);
          boxTop(typing.get(rhs));
          j.append("  invokevirtual java/util/HashMap/put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;\n");
          j.append("  pop\n");
        } else {
          j.append("  ; unsupported lvalue in assign\n");
        }
      }
      case "call" -> {
        @SuppressWarnings("unchecked")
        List<Object> args = (List<Object>) cmd.get("args");
        if (args != null) {
          for (Object arg : args) {
            if (arg instanceof AstNode argNode) {
              emitExpression(argNode);
            }
          }
        }
        String fn = String.valueOf(cmd.get("func"));
        j.append("  invokestatic LangMain/").append(fn).append("(");
        Symbols.FunSig sig = funSigs.get(fn);
        if (sig!=null) for (Type pt: sig.params) j.append(jdesc(pt));
        j.append(")V\n");
      }
      case "callWithRet" -> {
        @SuppressWarnings("unchecked")
        List<Object> args = (List<Object>) cmd.get("args");
        if (args != null) {
          for (Object arg : args) {
            if (arg instanceof AstNode argNode) {
              emitExpression(argNode);
            }
          }
        }
        String fnr = String.valueOf(cmd.get("func"));
        j.append("  invokestatic LangMain/").append(fnr).append("(");
        Symbols.FunSig sig2 = funSigs.get(fnr);
        if (sig2!=null) for (Type pt: sig2.params) j.append(jdesc(pt));
        j.append(")[Ljava/lang/Object;\n");

        int retsSlot = getLocalVar("__rets");
        j.append("  astore ").append(retsSlot).append("\n");

        @SuppressWarnings("unchecked")
        List<Object> lvalues = (List<Object>) cmd.get("lvalues");
        if (lvalues != null) {
          for (int i = 0; i < lvalues.size(); i++) {
            AstNode lv = (AstNode) lvalues.get(i);
            AstNode last = lastStep(lv);
            if (last == null) {
              j.append("  aload ").append(retsSlot).append("\n");
              emitIntConst(i);
              j.append("  aaload\n");
              Type tv = null;
              if (sig2!=null && i<sig2.returns.size()) tv = sig2.returns.get(i);
              castUnboxTop(tv);
              emitStore((String) lv.get("name"), kindFromType(tv));
            } else if ("arrayAccess".equals(last.get("type"))) {
              String base = (String) lv.get("name");
              j.append("  aload ").append(getLocalVar(base)).append("\n");
              emitExpression((AstNode) last.get("index"));
              j.append("  aload ").append(retsSlot).append("\n");
              emitIntConst(i);
              j.append("  aaload\n");
              Type tv2 = null; if (sig2!=null && i<sig2.returns.size()) tv2 = sig2.returns.get(i);
              castUnboxTop(tv2);
              JvmKind k2 = kindFromType(tv2);
              if (k2==JvmKind.F) j.append("  fastore\n"); else if (k2==JvmKind.A) j.append("  aastore\n"); else j.append("  iastore\n");
            } else if ("fieldAccess".equals(last.get("type"))) {
              String base = (String) lv.get("name");
              j.append("  aload ").append(getLocalVar(base)).append("\n");
              j.append("  ldc \"").append(last.get("field")).append("\"\n");
              j.append("  aload ").append(retsSlot).append("\n");
              emitIntConst(i);
              j.append("  aaload\n");
              j.append("  invokevirtual java/util/HashMap/put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;\n");
              j.append("  pop\n");
            } else {
              j.append("  ; unsupported lvalue in callWithRet\n");
            }
          }
        }
      }
      case "exprCmd" -> {
        emitExpression((AstNode) cmd.get("expr"));
        j.append("  pop\n");
      }
    }
  }
  
  private void emitExpression(AstNode expr) {
    if (expr == null) return;
    
    switch ((String) expr.get("type")) {
      case "int" -> {
        int value = (Integer) expr.get("value");
        if (value >= -1 && value <= 5) {
          j.append("  iconst_").append(value).append("\n");
        } else if (value >= -128 && value <= 127) {
          j.append("  bipush ").append(value).append("\n");
        } else if (value >= -32768 && value <= 32767) {
          j.append("  sipush ").append(value).append("\n");
        } else {
          j.append("  ldc ").append(value).append("\n");
        }
      }
      case "float" -> {
        double value = ((Number) expr.get("value")).doubleValue();
        j.append("  ldc ").append(value).append("\n");
      }
      case "bool" -> {
        boolean value = (Boolean) expr.get("value");
        j.append("  iconst_").append(value ? "1" : "0").append("\n");
      }
      case "char" -> {
        char value = (Character) expr.get("value");
        j.append("  bipush ").append((int) value).append("\n");
      }
      case "string" -> {
        String value = (String) expr.get("value");
        j.append("  ldc \"").append(value).append("\"\n");
      }
      case "null" -> {
        j.append("  aconst_null\n");
      }
      case "var" -> {
        String name = (String) expr.get("name");
        emitLoad(name);
      }
      case "un" -> {
        String op = (String) expr.get("op");
        emitExpression((AstNode) expr.get("expr"));
        if ("!".equals(op)) {
          j.append("  ifeq L").append(++labelCounter).append("\n  iconst_1\n  goto L").append(++labelCounter).append("\n  L").append(labelCounter-1).append(":\n  iconst_0\n  L").append(labelCounter).append(":\n");
        } else if ("-".equals(op)) {
          Type te = typing.get((AstNode) expr.get("expr"));
          if (te instanceof Type.Prim p && p==Type.Prim.FLOAT) j.append("  fneg\n"); else j.append("  ineg\n");
        } else if ("+".equals(op)) {}
      }
      case "bin" -> {
        String op = (String) expr.get("op");
        if ("&&".equals(op)) {
          String falseLabel = "L" + (++labelCounter);
          String endLabel = "L" + (++labelCounter);
          emitExpression((AstNode) expr.get("left"));
          j.append("  ifeq ").append(falseLabel).append("\n");
          emitExpression((AstNode) expr.get("right"));
          j.append("  ifeq ").append(falseLabel).append("\n");
          j.append("  iconst_1\n");
          j.append("  goto ").append(endLabel).append("\n");
          j.append(falseLabel).append(":\n");
          j.append("  iconst_0\n");
          j.append(endLabel).append(":\n");
        } else if ("||".equals(op)) {
          String trueLabel = "L" + (++labelCounter);
          String endLabel = "L" + (++labelCounter);
          emitExpression((AstNode) expr.get("left"));
          j.append("  ifne ").append(trueLabel).append("\n");
          emitExpression((AstNode) expr.get("right"));
          j.append("  ifne ").append(trueLabel).append("\n");
          j.append("  iconst_0\n");
          j.append("  goto ").append(endLabel).append("\n");
          j.append(trueLabel).append(":\n");
          j.append("  iconst_1\n");
          j.append(endLabel).append(":\n");
        } else {
          Type lt = typing.get((AstNode) expr.get("left"));
          Type rt = typing.get((AstNode) expr.get("right"));
          boolean useFloat = (lt instanceof Type.Prim lp && lp==Type.Prim.FLOAT) || (rt instanceof Type.Prim rp && rp==Type.Prim.FLOAT);
          emitExpression((AstNode) expr.get("left"));
          if (useFloat && !(lt instanceof Type.Prim lp && lp==Type.Prim.FLOAT)) j.append("  i2f\n");
          emitExpression((AstNode) expr.get("right"));
          if (useFloat && !(rt instanceof Type.Prim rp2 && rp2==Type.Prim.FLOAT)) j.append("  i2f\n");

          if (Set.of("+","-","*","/","%").contains(op)) {
            if (useFloat) {
              switch (op) {
                case "+" -> j.append("  fadd\n");
                case "-" -> j.append("  fsub\n");
                case "*" -> j.append("  fmul\n");
                case "/" -> j.append("  fdiv\n");
                case "%" -> j.append("  ; unsupported float %\n");
              }
            } else {
              switch (op) {
                case "+" -> j.append("  iadd\n");
                case "-" -> j.append("  isub\n");
                case "*" -> j.append("  imul\n");
                case "/" -> j.append("  idiv\n");
                case "%" -> j.append("  irem\n");
              }
            }
          } else {
            String trueLabel = "L" + (++labelCounter);
            String endLabel = "L" + (++labelCounter);
            if (useFloat) {
              j.append("  fcmpl\n");
              switch (op) {
                case "==" -> j.append("  ifeq ").append(trueLabel).append("\n");
                case "!=" -> j.append("  ifne ").append(trueLabel).append("\n");
                case "<"  -> j.append("  iflt ").append(trueLabel).append("\n");
                case ">"  -> j.append("  ifgt ").append(trueLabel).append("\n");
                case "<=" -> j.append("  ifle ").append(trueLabel).append("\n");
                case ">=" -> j.append("  ifge ").append(trueLabel).append("\n");
              }
            } else {
              switch (op) {
                case "==" -> j.append("  if_icmpeq ").append(trueLabel).append("\n");
                case "!=" -> j.append("  if_icmpne ").append(trueLabel).append("\n");
                case "<"  -> j.append("  if_icmplt ").append(trueLabel).append("\n");
                case ">"  -> j.append("  if_icmpgt ").append(trueLabel).append("\n");
                case "<=" -> j.append("  if_icmple ").append(trueLabel).append("\n");
                case ">=" -> j.append("  if_icmpge ").append(trueLabel).append("\n");
              }
            }
            j.append("  iconst_0\n  goto ").append(endLabel).append("\n");
            j.append(trueLabel).append(":\n  iconst_1\n");
            j.append(endLabel).append(":\n");
          }
        }
      }
      case "arrayAccess" -> {
        AstNode arr = (AstNode) expr.get("array");
        Type arrT = typing.get(arr);
        if ("var".equals(arr.get("type"))) emitLoad((String) arr.get("name")); else emitExpression(arr);
        emitExpression((AstNode) expr.get("index"));
        if (arrT instanceof Type.Array a) {
          if (a.elem instanceof Type.Prim p && p==Type.Prim.FLOAT) j.append("  faload\n");
          else if (a.elem instanceof Type.Prim) j.append("  iaload\n");
          else j.append("  aaload\n");
        } else {
          j.append("  aaload\n");
          Type elemT = typing.get(expr);
          castUnboxTop(elemT);
        }
      }
      case "fieldAccess" -> {
        emitExpression((AstNode) expr.get("obj"));
        j.append("  ldc \"").append(expr.get("field")).append("\"\n");
        j.append("  invokevirtual java/util/HashMap/get(Ljava/lang/Object;)Ljava/lang/Object;\n");
        Type ft = typing.get(expr);
        castUnboxTop(ft);
      }
      case "arrayLit" -> {
        @SuppressWarnings("unchecked")
        List<Object> els = (List<Object>) expr.get("elements");
        int n = els == null ? 0 : els.size();
        emitIntConst(n);

        Type t = typing.get(expr);
        Type elem = (t instanceof Type.Array a) ? a.elem : Type.Prim.INT;
        JvmKind ek = kindFromType(elem);

        if (ek == JvmKind.F) j.append("  newarray float\n");
        else if (ek == JvmKind.A) j.append("  anewarray java/lang/Object\n");
        else j.append("  newarray int\n");

        for (int i = 0; i < n; i++) {
          j.append("  dup\n");
          emitIntConst(i);
          AstNode e = (AstNode) els.get(i);
          emitExpression(e);
          if (ek == JvmKind.A) {
            boxTop(typing.get(e));
          }
          if (ek == JvmKind.F) j.append("  fastore\n");
          else if (ek == JvmKind.A) j.append("  aastore\n");
          else j.append("  iastore\n");
        }
      }
      case "newRec" -> {
        String typeName = extractIdName(expr.get("recType"));
        j.append("  new java/util/HashMap\n");
        j.append("  dup\n");
        j.append("  invokespecial java/util/HashMap/<init>()V\n");
      }
      case "newArr" -> {
        emitExpression((AstNode) expr.get("size"));
        Type t = typing.get(expr);
        if (t instanceof Type.Array a) {
          if (a.elem instanceof Type.Prim p && p==Type.Prim.FLOAT) j.append("  newarray float\n");
          else if (a.elem instanceof Type.Prim p2 && (p2==Type.Prim.INT || p2==Type.Prim.BOOL || p2==Type.Prim.CHAR)) j.append("  newarray int\n");
          else j.append("  anewarray java/lang/Object\n");
        } else {
          j.append("  anewarray java/lang/Object\n");
        }
      }
      case "callExpr" -> {
        AstNode funcNode = (AstNode) expr.get("func");
        String funcName = (String) funcNode.get("name");
        
        @SuppressWarnings("unchecked")
        List<Object> args = (List<Object>) expr.get("args");
        if (args != null) {
          for (Object arg : args) {
            if (arg instanceof AstNode argNode) {
              emitExpression(argNode);
            }
          }
        }
        
        j.append("  invokestatic LangMain/").append(funcName).append("(");
        Symbols.FunSig sig = funSigs.get(funcName);
        if (sig!=null) for (Type pt: sig.params) j.append(jdesc(pt));
        Type rt = typing.get(expr);
        if (rt instanceof Type.Product) j.append(")[Ljava/lang/Object;\n");
        else { j.append(")").append(jdesc(rt==null?Type.NullT.INSTANCE:rt)).append("\n"); }
      }
    }
  }

  private void boxTop(Type t){
    if (t instanceof Type.Prim p) switch (p){
      case INT -> j.append("  invokestatic java/lang/Integer/valueOf(I)Ljava/lang/Integer;\n");
      case FLOAT -> j.append("  invokestatic java/lang/Float/valueOf(F)Ljava/lang/Float;\n");
      case BOOL -> j.append("  invokestatic java/lang/Boolean/valueOf(Z)Ljava/lang/Boolean;\n");
      case CHAR -> j.append("  invokestatic java/lang/Character/valueOf(C)Ljava/lang/Character;\n");
    }
  }

  private void castUnboxTop(Type t){
    if (t instanceof Type.Prim p) switch (p){
      case INT -> j.append("  checkcast java/lang/Integer\n  invokevirtual java/lang/Integer/intValue()I\n");
      case FLOAT -> j.append("  checkcast java/lang/Float\n  invokevirtual java/lang/Float/floatValue()F\n");
      case BOOL -> j.append("  checkcast java/lang/Boolean\n  invokevirtual java/lang/Boolean/booleanValue()Z\n");
      case CHAR -> j.append("  checkcast java/lang/Character\n  invokevirtual java/lang/Character/charValue()C\n");
    }
    else if (t instanceof Type.StringT) j.append("  checkcast java/lang/String\n");
  }

  private JvmKind kindFromType(Type t){
    if (t instanceof Type.Prim p) return p==Type.Prim.FLOAT ? JvmKind.F : JvmKind.I;
    if (t==null) return JvmKind.I;
    return JvmKind.A;
  }
  
  private String extractIdName(Object id) {
    if (id instanceof String s) return s;
    if (id instanceof AstNode n) return (String) n.get("name");
    return String.valueOf(id);
  }
  
  private int getLocalVar(String name) {
    Integer slot = localVars.get(name);
    if (slot != null) return slot;
    int id = ++localCounter;
    localVars.put(name, id);
    return id;
  }

  private void emitIntConst(int value){
    if (value >= -1 && value <= 5) j.append("  iconst_").append(value).append("\n");
    else if (value >= -128 && value <= 127) j.append("  bipush ").append(value).append("\n");
    else if (value >= -32768 && value <= 32767) j.append("  sipush ").append(value).append("\n");
    else j.append("  ldc ").append(value).append("\n");
  }

  private AstNode lastStep(AstNode lv){
    AstNode cur = lv;
    AstNode nxt = (AstNode) cur.get("next");
    if (nxt==null) return null;
    while (nxt.get("next") != null) nxt = (AstNode) nxt.get("next");
    return nxt;
  }

  private void scopePush(){
    scopeStack.push(new HashMap<>(localVars));
  }
  private void scopePop(){
    Map<String,Integer> prev = scopeStack.pop();
    localVars.clear(); localVars.putAll(prev);
  }
}
