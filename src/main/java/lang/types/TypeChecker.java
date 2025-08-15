/*
 * allan amaral - 201935001
 * carlos mattos - 201935003
 */

package lang.types;

import lang.ast.AstNode;
import java.util.*;
import java.util.IdentityHashMap;
import java.util.Collections;
import static lang.types.Type.*;

public class TypeChecker {
  private final Diagnostics diags;
  private final Symbols syms = new Symbols();

  private final Set<String> abstractRecords = new HashSet<>();
  private String currentOwner = null;

  private final IdentityHashMap<AstNode, Type> typing = new IdentityHashMap<>();

  public Map<AstNode, Type> typing() {
    return Collections.unmodifiableMap(typing);
  }

  public Map<AstNode, Type> getTyping() { return typing(); }
  public Map<String, Symbols.FunSig> getFunSigs() { return syms.funSigsView(); }
  public Optional<Type.Record> getRecord(String name) { return syms.getRecord(name); }

  public TypeChecker(Diagnostics d){ this.diags = d; }

  public boolean check(AstNode prog){
    if (!"prog".equals(extractTypeName(prog.get("type")))) { diags.error("invalid AST"); return false; }
    @SuppressWarnings("unchecked")
    List<Object> defs = (List<Object>)prog.get("definitions");
    for (Object d: defs) if (d instanceof AstNode n) {
      switch (extractTypeName(n.get("type"))) {
        case "dataDecl" -> registerRecord(n);
        case "funDecl"  -> registerFunSig(n);
      }
    }
    for (Object d: defs) if (d instanceof AstNode n && "dataDecl".equals(extractTypeName(n.get("type")))) {
      @SuppressWarnings("unchecked") List<Object> members = (List<Object>) n.get("members");
      if (members!=null) for (Object m: members) if (m instanceof AstNode fm && "funDecl".equals(extractTypeName(fm.get("type")))) {
        registerFunSig(fm);
      }
    }
    var mainSigOpt = syms.getFun("main");
    if (mainSigOpt.isEmpty()) {
      diags.error("no main");
    } else {
      var ms = mainSigOpt.get();
      if (!ms.params.isEmpty()) diags.error("main must have no params");
      if (!ms.returns.isEmpty()) diags.error("main must return void");
    }
    for (Object d: defs) if (d instanceof AstNode n && "funDecl".equals(extractTypeName(n.get("type")))) checkFunOwned(n, null);
    for (Object d: defs) if (d instanceof AstNode n && "dataDecl".equals(extractTypeName(n.get("type")))) {
      String ownerName = extractTypeName(n.get("name"));
      @SuppressWarnings("unchecked") List<Object> members = (List<Object>) n.get("members");
      if (members!=null) for (Object m: members) if (m instanceof AstNode fm && "funDecl".equals(extractTypeName(fm.get("type")))) {
        checkFunOwned(fm, ownerName);
      }
    }
    return diags.ok();
  }

  private void registerRecord(AstNode data){
    String name = extractTypeName(data.get("name"));
    boolean isAbs = Boolean.TRUE.equals(data.get("isAbstract"));
    if (isAbs) abstractRecords.add(name);
    Map<String,Type> fields = new HashMap<>();
    @SuppressWarnings("unchecked") List<Object> members = (List<Object>) data.get("members");
    for (Object m: members) if (m instanceof AstNode dm) {
      Object memberTypeField = dm.get("type");
      if (memberTypeField instanceof AstNode typeAst) {
        String fname = extractTypeName(dm.get("name"));
        Type ftype = typeOf(typeAst);
        fields.put(fname, ftype);
      }
    }
    syms.putRecord(new Type.Record(name, fields));
  }

  private void registerFunSig(AstNode f){
    String name = extractTypeName(f.get("name"));
    List<Type> params = new ArrayList<>();
    @SuppressWarnings("unchecked") List<Object> ps = (List<Object>) f.get("params");
    if (ps != null) for (Object p: ps) if (p instanceof AstNode pn) params.add(typeOf((AstNode) pn.get("type")));
    List<Type> rets = new ArrayList<>();
    @SuppressWarnings("unchecked") List<Object> rt = (List<Object>) f.get("returnTypes");
    if (rt != null) for (Object t: rt) if (t instanceof AstNode tn) rets.add(typeOf(tn));
    syms.putFun(name, new Symbols.FunSig(params, rets));
  }

  private void checkFunOwned(AstNode f, String owner){
    String name = extractTypeName(f.get("name"));
    var sig = syms.getFun(name).orElse(null);
    syms.push();
    String prevOwner = currentOwner;
    currentOwner = owner;
    @SuppressWarnings("unchecked") List<Object> ps = (List<Object>) f.get("params");
    if (ps != null) for (int i=0;i<ps.size();i++){
      AstNode p = (AstNode) ps.get(i);
      syms.putVar(extractTypeName(p.get("name")), sig.params.get(i));
    }
    checkCmd((AstNode) f.get("body"), sig);
    if (!sig.returns.isEmpty()) {
      AstNode body = (AstNode) f.get("body");
      if (body == null || !guaranteesReturn(body)) {
        diags.error("missing return on some path in " + name);
      }
    }
    currentOwner = prevOwner;
    syms.pop();
  }

  private boolean guaranteesReturn(AstNode cmd) {
    String k = extractTypeName(cmd.get("type"));
    switch (k) {
      case "return" -> { return true; }
      case "block" -> {
        @SuppressWarnings("unchecked")
        List<Object> cs = (List<Object>) cmd.get("commands");
        if (cs == null) return false;
        for (Object c : cs) if (c instanceof AstNode n && guaranteesReturn(n)) return true;
        return false;
      }
      case "if" -> {
        AstNode thenC = (AstNode) cmd.get("then");
        AstNode elseC = (AstNode) cmd.get("else");
        boolean thenRet = thenC != null && guaranteesReturn(thenC);
        boolean elseRet = elseC != null && guaranteesReturn(elseC);
        return thenRet && elseRet;
      }
      case "iterate" -> {
        return false;
      }
      default -> { return false; }
    }
  }

  private void checkCmd(AstNode cmd, Symbols.FunSig current){
    switch (extractTypeName(cmd.get("type"))) {
      case "block" -> {
        syms.push();
        @SuppressWarnings("unchecked") List<Object> cs = (List<Object>) cmd.get("commands");
        if (cs!=null) for (Object c: cs) if (c instanceof AstNode n) checkCmd(n, current);
        syms.pop();
      }
      case "if" -> {
        var tcond = typeOfExpr((AstNode) cmd.get("condition"));
        if (!(tcond instanceof Prim p && p==Prim.BOOL)) diags.error("if expects Bool");
        if (cmd.get("then")!=null) checkCmd((AstNode) cmd.get("then"), current);
        if (cmd.get("else")!=null) checkCmd((AstNode) cmd.get("else"), current);
      }
      case "iterate" -> {
        if (cmd.get("id")!=null) {
          var tbase = typeOfExpr((AstNode) cmd.get("expr"));
          if (tbase instanceof Array arr) {
          ensureLValueType((AstNode) cmd.get("id"), arr.elem);
        } else if (tbase instanceof Prim p && p==Prim.INT) {
          ensureLValueType((AstNode) cmd.get("id"), Prim.INT);
        } else diags.error("iterate expects Int or Array");
          checkCmd((AstNode) cmd.get("body"), current);
        } else {
          var t = typeOfExpr((AstNode) cmd.get("expr"));
          if (!(t instanceof Prim p && p==Prim.INT)) diags.error("iterate(N) expects Int");
          checkCmd((AstNode) cmd.get("body"), current);
        }
      }
      case "print" -> typeOfExpr((AstNode) cmd.get("expr"));
      case "read" -> {
        var lv = (AstNode) cmd.get("lvalue");
        ensureLValueType(lv, Prim.INT);
      }
      case "return" -> {
        @SuppressWarnings("unchecked") List<Object> es = (List<Object>) cmd.get("exprs");
        List<Type> actual = new ArrayList<>();
        if (es!=null) for (Object e: es) actual.add(typeOfExpr((AstNode) e));
        if (actual.size() != current.returns.size()) diags.error("return arity mismatch");
        else for (int i=0;i<actual.size();i++) if (!compatible(current.returns.get(i), actual.get(i))) diags.error("return type mismatch");
      }
      case "assign" -> {
        var lv = (AstNode) cmd.get("lvalue");
        var rt = typeOfExpr((AstNode) cmd.get("expr"));
        ensureLValueType(lv, rt);
      }
      case "call" -> {
        String fn = extractTypeName(cmd.get("func"));
        var sig = syms.getFun(fn).orElse(null);
        if (sig==null) { diags.error("unknown function "+fn); break; }
        @SuppressWarnings("unchecked") List<Object> as = (List<Object>) cmd.get("args");
        if ((as==null?0:as.size()) != sig.params.size()) diags.error("arity mismatch in call "+fn);
        else for (int i=0;i<sig.params.size();i++){
          var at = typeOfExpr((AstNode) as.get(i));
          if (!compatible(sig.params.get(i), at)) diags.error("arg type mismatch in call "+fn);
        }
      }
      case "callWithRet" -> {
        String fn = extractTypeName(cmd.get("func"));
        var sig = syms.getFun(fn).orElse(null);
        if (sig==null) { diags.error("unknown function "+fn); break; }
        @SuppressWarnings("unchecked") List<Object> as = (List<Object>) cmd.get("args");
        @SuppressWarnings("unchecked") List<Object> lvs = (List<Object>) cmd.get("lvalues");
        if ((as==null?0:as.size()) != sig.params.size()) diags.error("arity mismatch in call "+fn);
        else for (int i=0;i<sig.params.size();i++){
          var at = typeOfExpr((AstNode) as.get(i));
          if (!compatible(sig.params.get(i), at)) diags.error("arg type mismatch in call "+fn);
        }
        if (lvs.size()!=sig.returns.size()) diags.error("return arity mismatch in "+fn);
        else for (int i=0;i<lvs.size();i++) ensureLValueType((AstNode) lvs.get(i), sig.returns.get(i));
      }
      case "exprCmd" -> typeOfExpr((AstNode) cmd.get("expr"));
    }
  }

  private Type typeOf(AstNode t){
    String k = extractTypeName(t.get("type"));
    return switch (k) {
      case "intType" -> Prim.INT;
      case "floatType" -> Prim.FLOAT;
      case "boolType" -> Prim.BOOL;
      case "charType" -> Prim.CHAR;
      case "arrayType" -> new Array(typeOf((AstNode)t.get("elementType")));
      case "productType" -> new Product(List.of(typeOf((AstNode)t.get("left")), typeOf((AstNode)t.get("right"))));
      case "simpleType" -> {
        String name = extractTypeName(t.get("name"));
        var rec = syms.getRecord(name).orElse(null);
        if (rec==null) { diags.error("unknown type "+name); yield Prim.INT; }
        yield rec;
      }
      case "type" -> {
        String name = extractTypeName(t.get("name"));
        yield switch (name) {
          case "Int" -> Prim.INT; case "Float" -> Prim.FLOAT; case "Bool" -> Prim.BOOL; case "Char" -> Prim.CHAR;
          default -> syms.getRecord(name).orElseGet(() -> { diags.error("unknown type "+name); return new Type.Record(name, Map.of()); });
        };
      }
      default -> { diags.error("unknown type node "+k); yield Prim.INT; }
    };
  }

  private Type typeOfExpr(AstNode e){
    String k = extractTypeName(e.get("type"));
    return switch (k) {
      case "int" -> { typing.put(e, Prim.INT); yield Prim.INT; }
      case "float" -> { typing.put(e, Prim.FLOAT); yield Prim.FLOAT; }
      case "bool" -> { typing.put(e, Prim.BOOL); yield Prim.BOOL; }
      case "char" -> { typing.put(e, Prim.CHAR); yield Prim.CHAR; }
      case "string" -> { typing.put(e, StringT.INSTANCE); yield StringT.INSTANCE; }
      case "null" -> { typing.put(e, NullT.INSTANCE); yield NullT.INSTANCE; }
      case "var" -> {
        Object nameObj = e.get("name");
        String name = extractTypeName(nameObj);
        var fromScope = syms.lookupVar(name);
        if (fromScope.isPresent()) {
          Type t = fromScope.get();
          typing.put(e, t);
          yield t;
        }
        if (currentOwner != null) {
          var recOpt = syms.getRecord(currentOwner);
          if (recOpt.isPresent()) {
            var rec = recOpt.get();
            var fieldType = rec.fields.get(name);
            if (fieldType != null) {
              typing.put(e, fieldType);
              yield fieldType;
            }
          }
        }
        diags.error("undeclared " + name);
        typing.put(e, Prim.INT);
        yield Prim.INT;
      }
      case "un" -> {
        String op = extractTypeName(e.get("op"));
        var t = typeOfExpr((AstNode) e.get("expr"));
        if (op.equals("!")) { if (!(t instanceof Prim p && p==Prim.BOOL)) diags.error("! expects Bool"); typing.put(e, Prim.BOOL); yield Prim.BOOL; }
        if (op.equals("-") || op.equals("+")) { if (!isNumeric(t)) diags.error("unary expects numeric"); typing.put(e, t); yield t; }
        typing.put(e, t); yield t;
      }
      case "bin" -> { Type t = binType(e); typing.put(e, t); yield t; }
      case "arrayAccess" -> {
        var a = typeOfExpr((AstNode)e.get("array"));
        var idxT = typeOfExpr((AstNode)e.get("index"));
        if (!(idxT instanceof Prim p && p==Prim.INT)) diags.error("index must be Int");
        if (a instanceof Array arr) { typing.put(e, arr.elem); yield arr.elem; }
        if (a instanceof Product prod) {
          Object idxNode = ((AstNode)e.get("index")).get("type");
          if ("int".equals(idxNode)) {
            int idx = (Integer) ((AstNode)e.get("index")).get("value");
            if (idx >= 0 && idx < prod.elems.size()) {
              Type t = prod.elems.get(idx);
              typing.put(e, t);
              yield t;
            } else {
              diags.error("product index out of bounds: " + idx);
              typing.put(e, Prim.INT); yield Prim.INT;
            }
          } else {
            diags.error("product selection requires literal index");
            typing.put(e, Prim.INT); yield Prim.INT;
          }
        }
        diags.error("indexing non-array"); typing.put(e, Prim.INT); yield Prim.INT;
      }
      case "fieldAccess" -> {
        var rec = typeOfExpr((AstNode)e.get("obj"));
        if (rec instanceof Type.Record r) {
          String f = extractTypeName(e.get("field"));
          var ft = r.fields.get(f);
          if (ft==null) { diags.error("unknown field "+f+" in "+r.name); typing.put(e, Prim.INT); yield Prim.INT; }
          if (abstractRecords.contains(r.name) && (currentOwner==null || !currentOwner.equals(r.name))) {
            diags.error("forbidden field access on abstract type "+r.name);
          }
          typing.put(e, ft);
          yield ft;
        }
        diags.error("field access on non-record"); typing.put(e, Prim.INT); yield Prim.INT;
      }
      case "arrayLit" -> {
        @SuppressWarnings("unchecked") List<Object> els = (List<Object>) e.get("elements");
        if (els==null || els.isEmpty()) { Type t = new Array(Prim.INT); typing.put(e, t); yield t; }
        else {
          Type t0 = typeOfExpr((AstNode) els.get(0));
          for (int i=1;i<els.size();i++) t0 = lub(t0, typeOfExpr((AstNode) els.get(i)));
          Type t = new Array(t0);
          typing.put(e, t);
          yield t;
        }
      }
      case "newRec" -> {
        Object typeObj = e.get("recType");
        String name = extractTypeName(typeObj);
        var rec = syms.getRecord(name).orElseGet(() -> {
          diags.error("unknown record " + name);
          return new Type.Record(name, Map.of());
        });
        if (abstractRecords.contains(name)) {
          diags.error("cannot instantiate abstract type " + name);
        }
        typing.put(e, rec);
        yield rec;
      }
      case "newArr" -> { 
        Object typeNode = e.get("elemType");
        if (!(typeNode instanceof AstNode)) {
          diags.error("invalid type node in newArr");
          Type t = new Array(Prim.INT); typing.put(e, t); yield t;
        }
        var et = typeOf((AstNode) typeNode); 
        var sz = typeOfExpr((AstNode) e.get("size"));
        if (!(sz instanceof Prim p && p==Prim.INT)) diags.error("array size must be Int");
        Type t = new Array(et);
        typing.put(e, t);
        yield t;
      }
      case "callExpr" -> {
        var fnVar = (AstNode) e.get("func");
        if (!"var".equals(fnVar.get("type"))) { diags.error("call of non-name"); yield new Product(List.of()); }
        String fn = extractTypeName(fnVar.get("name"));
        var sig = syms.getFun(fn).orElseGet(() -> { diags.error("unknown function "+fn); return new Symbols.FunSig(List.of(), List.of()); });
        @SuppressWarnings("unchecked") List<Object> as = (List<Object>) e.get("args");
        if ((as==null?0:as.size()) != sig.params.size()) diags.error("arity mismatch in call "+fn);
        else for (int i=0;i<sig.params.size();i++){
          var at = typeOfExpr((AstNode) as.get(i));
          if (!compatible(sig.params.get(i), at)) diags.error("arg type mismatch in call "+fn);
        }
        Type t = new Product(sig.returns);
        typing.put(e, t);
        yield t;
      }
      default -> { typing.put(e, Prim.INT); yield Prim.INT; }
    };
  }

  private void ensureLValueType(AstNode lv, Type want) {
    String name = extractTypeName(lv.get("name"));
    boolean hasChain = lv.get("next") != null;
    var cur = syms.lookupVar(name);
    if (!hasChain && cur.isEmpty()) { syms.putVar(name, want); return; }
    putOrCheckLValueType(lv, want);
  }

  private void putOrCheckLValueType(AstNode lv, Type want) {
    if (!"lvalue".equals(extractTypeName(lv.get("type")))) {
      diags.error("invalid lvalue type: " + lv.get("type")); return;
    }
    String name = extractTypeName(lv.get("name"));
    var baseT = syms.lookupVar(name).orElse(null);
    if (baseT == null) { diags.error("undeclared " + name); return; }

    AstNode curNode = lv;
    while (curNode.get("next") != null) {
      AstNode step = (AstNode) curNode.get("next");
      String stepK = extractTypeName(step.get("type"));
      switch (stepK) {
        case "arrayAccess" -> {
          var idxT = typeOfExpr((AstNode) step.get("index"));
          if (!(idxT instanceof Prim p && p==Prim.INT)) diags.error("array index must be Int");
          if (baseT instanceof Array arr) {
            baseT = arr.elem;
          } else if (baseT instanceof Product prod) {
            Object idxNodeType = ((AstNode) step.get("index")).get("type");
            if ("int".equals(idxNodeType)) {
              int i = (Integer)((AstNode) step.get("index")).get("value");
              if (i>=0 && i<prod.elems.size()) baseT = prod.elems.get(i);
              else diags.error("product index out of bounds: " + i);
            } else {
              diags.error("product selection requires literal index");
            }
          } else {
            diags.error("indexing non-array"); return;
          }
        }
        case "fieldAccess" -> {
          if (baseT instanceof Type.Record r) {
            String f = extractTypeName(step.get("field"));
            var ft = r.fields.get(f);
            if (ft == null) diags.error("unknown field " + f + " in " + r.name);
            if (abstractRecords.contains(r.name) && (currentOwner==null || !currentOwner.equals(r.name))) {
              diags.error("forbidden field access on abstract type " + r.name);
            }
            baseT = ft;
          } else {
            diags.error("field access on non-record");
            return;
          }
        }
        default -> diags.error("invalid lvalue segment: " + stepK);
      }
      curNode = step;
    }
    if (!compatible(baseT, want)) diags.error("assignment type mismatch");
  }

  private static boolean isNumeric(Type t){ return t instanceof Prim p && (p==Prim.INT || p==Prim.FLOAT); }
  private static boolean compatible(Type expected, Type actual){
    if (expected.equals(actual)) return true;
    if (expected==Prim.FLOAT && actual==Prim.INT) return true;
    if (expected instanceof Array ea && actual instanceof Array aa) return compatible(ea.elem, aa.elem);
    if (actual==NullT.INSTANCE && (expected instanceof Array || expected instanceof Type.Record || expected==StringT.INSTANCE)) return true;
    return false;
  }
  private static Type lub(Type a, Type b){
    if (a.equals(b)) return a;
    if (isNumeric(a) && isNumeric(b)) return Prim.FLOAT;
    return a;
  }
  private static String extractTypeName(Object o){
    if (o instanceof String s) return s;
    if (o instanceof AstNode n) {
      Object nameObj = n.get("name");
      if (nameObj instanceof String name) return name;
      if (nameObj instanceof AstNode nameNode) return extractTypeName(nameNode);
      return String.valueOf(nameObj);
    }
    return String.valueOf(o);
  }
  private Type binType(AstNode e){
    String op = extractTypeName(e.get("op"));
    Type lt = typeOfExpr((AstNode) e.get("left"));
    Type rt = typeOfExpr((AstNode) e.get("right"));
    if (Set.of("+","-","*","/","%").contains(op)) {
      if (!isNumeric(lt) || !isNumeric(rt)) { diags.error("numeric op on non-numeric"); return Prim.INT; }
      if (op.equals("%") && !(lt==Prim.INT && rt==Prim.INT)) diags.error("% requires Int");
      return (lt==Prim.FLOAT || rt==Prim.FLOAT) ? Prim.FLOAT : Prim.INT;
    }
    if (Set.of("==","!=","<",">","<=",">=").contains(op)) return Prim.BOOL;
    if (Set.of("&&","||").contains(op)) {
      if (!(lt==Prim.BOOL && rt==Prim.BOOL)) diags.error("bool op expects Bool");
      return Prim.BOOL;
    }
    return Prim.INT;
  }
}
