/*
 * allan amaral - 201935001
 * carlos mattos - 201935003
 */

package lang.src;

import lang.ast.AstNode;
import java.util.*;

public class JavaGen {
    private final StringBuilder out = new StringBuilder();
    private int tempCounter = 0;

    public String emit(AstNode prog) {
        out.append("import java.util.*;\n");
        out.append("public class LangSource {\n");
        emitTuples(4);
        if ("prog".equals(prog.get("type"))) {
            @SuppressWarnings("unchecked")
            List<Object> defs = (List<Object>) prog.get("definitions");
            if (defs != null) {
                for (Object d : defs) {
                    if (!(d instanceof AstNode)) continue;
                    AstNode n = (AstNode) d;
                    String nt = String.valueOf(n.get("type"));
                    if ("funDecl".equals(nt)) {
                        emitFunction(n);
                    } else if ("dataDecl".equals(nt)) {
                        @SuppressWarnings("unchecked")
                        List<Object> members = (List<Object>) n.get("members");
                        if (members != null) {
                            for (Object m : members) {
                                if (m instanceof AstNode fn && "funDecl".equals(fn.get("type"))) {
                                    emitFunction(fn);
                                }
                            }
                        }
                    }
                }
                emitMain(defs);
            }
        }
        out.append("}\n");
        return out.toString();
    }

    private void emitTuples(int n){
        for (int k=2;k<=n;k++){
            out.append("  public static final class Tuple").append(k).append("<");
            for (int i=0;i<k;i++){ if (i>0) out.append(","); out.append("A").append(i); }
            out.append("> {\n");
            for (int i=0;i<k;i++) out.append("    public final A").append(i).append(" _").append(i).append(";\n");
            out.append("    public Tuple").append(k).append("(");
            for (int i=0;i<k;i++){ if (i>0) out.append(","); out.append("A").append(i).append(" a").append(i); }
            out.append("){");
            for (int i=0;i<k;i++) out.append("this._").append(i).append("=a").append(i).append(";");
            out.append("}\n  }\n");
        }
    }

    private void emitFunction(AstNode f){
        String name = String.valueOf(f.get("name"));
        out.append("  public static Object ").append(name).append("(");
        @SuppressWarnings("unchecked") List<Object> ps = (List<Object>) f.get("params");
        for (int i=0;i<(ps==null?0:ps.size());i++){
            if (i>0) out.append(", ");
            AstNode p = (AstNode) ps.get(i);
            out.append("Object ").append(String.valueOf(p.get("name")));
        }
        out.append(") {\n");
        emitCmd((AstNode) f.get("body"));
        out.append("    return null;\n");
        out.append("  }\n");
    }

    private void emitMain(List<Object> defs){
        out.append("  public static void main(String[] args) {\n");
        for (Object d: defs) if (d instanceof AstNode n && "funDecl".equals(n.get("type")) && "main".equals(n.get("name"))) {
            emitCmd((AstNode) n.get("body"));
        }
        out.append("  }\n");
    }

    private void emitCmd(AstNode cmd){
        switch (String.valueOf(cmd.get("type"))){
            case "block" -> {
                @SuppressWarnings("unchecked") List<Object> cs = (List<Object>) cmd.get("commands");
                if (cs!=null) for (Object c: cs) if (c instanceof AstNode n) emitCmd(n);
            }
            case "if" -> {
                String t = emitExpr((AstNode) cmd.get("condition"));
                out.append("    if (toBool(").append(t).append(")) {\n");
                if (cmd.get("then")!=null) emitCmd((AstNode) cmd.get("then"));
                out.append("    } else {\n");
                if (cmd.get("else")!=null) emitCmd((AstNode) cmd.get("else"));
                out.append("    }\n");
            }
            case "iterate" -> {
                if (cmd.get("id")!=null){
                    AstNode id = (AstNode) cmd.get("id");
                    String base = emitExpr((AstNode) cmd.get("expr"));
                    String it = fresh();
                    out.append("    for (Object ").append(it).append(" : toIterable(").append(base).append(")) {\n");
                    out.append("      ").append(String.valueOf(id.get("name"))).append(" = ").append(it).append(";\n");
                    emitCmd((AstNode) cmd.get("body"));
                    out.append("    }\n");
                } else {
                    String n = emitExpr((AstNode) cmd.get("expr"));
                    String i = fresh();
                    out.append("    for (int ").append(i).append("=0; ").append(i).append("<toInt(").append(n).append("); ").append(i).append("++){\n");
                    emitCmd((AstNode) cmd.get("body"));
                    out.append("    }\n");
                }
            }
            case "print" -> {
                String v = emitExpr((AstNode) cmd.get("expr"));
                out.append("    System.out.println(").append(v).append(");\n");
            }
            case "read" -> {
                AstNode lv = (AstNode) cmd.get("lvalue");
                out.append("    {")
                   .append("java.util.Scanner __sc=new java.util.Scanner(System.in);")
                   .append(String.valueOf(lv.get("name"))).append(" = __sc.nextInt();}")
                   .append("\n");
            }
            case "return" -> {
                @SuppressWarnings("unchecked") List<Object> es = (List<Object>) cmd.get("exprs");
                if (es==null || es.isEmpty()) { out.append("    return null;\n"); }
                else if (es.size()==1) { out.append("    return ").append(emitExpr((AstNode) es.get(0))).append(";\n"); }
                else {
                    out.append("    return new Object[]{");
                    for (int i=0;i<es.size();i++){ if (i>0) out.append(", "); out.append(emitExpr((AstNode) es.get(i))); }
                    out.append("};\n");
                }
            }
            case "assign" -> {
                AstNode lv = (AstNode) cmd.get("lvalue");
                AstNode rhs = (AstNode) cmd.get("expr");
                if (lv.get("next")==null){
                    out.append("    ").append(String.valueOf(lv.get("name"))).append(" = ").append(emitExpr(rhs)).append(";\n");
                } else {
                    AstNode last = lastStep(lv);
                    if ("fieldAccess".equals(last.get("type"))){
                        out.append("    ((java.util.HashMap)").append(String.valueOf(lv.get("name"))).append(")").append(".put(\"")
                           .append(String.valueOf(last.get("field"))).append("\", ").append(emitExpr(rhs)).append(");\n");
                    } else if ("arrayAccess".equals(last.get("type"))){
                        String arr = String.valueOf(lv.get("name"));
                        String idx = emitExpr((AstNode) last.get("index"));
                        String val = emitExpr(rhs);
                        out.append("    ((Object[])").append(arr).append(")[toInt(").append(idx).append(")] = ").append(val).append(";\n");
                    }
                }
            }
            case "call" -> {
                @SuppressWarnings("unchecked") List<Object> as = (List<Object>) cmd.get("args");
                out.append("    ").append(String.valueOf(cmd.get("func"))).append("(");
                if (as!=null) for (int i=0;i<as.size();i++){ if (i>0) out.append(", "); out.append(emitExpr((AstNode) as.get(i))); }
                out.append(");\n");
            }
            case "callWithRet" -> {
                String fn = String.valueOf(cmd.get("func"));
                @SuppressWarnings("unchecked") List<Object> as = (List<Object>) cmd.get("args");
                String tmp = fresh();
                out.append("    Object[] ").append(tmp).append(" = (Object[])").append(fn).append("(");
                if (as!=null) for (int i=0;i<as.size();i++){ if (i>0) out.append(", "); out.append(emitExpr((AstNode) as.get(i))); }
                out.append(");\n");
                @SuppressWarnings("unchecked") List<Object> lvs = (List<Object>) cmd.get("lvalues");
                for (int i=0;i<lvs.size();i++){
                    AstNode lv = (AstNode) lvs.get(i);
                    out.append("    ").append(String.valueOf(lv.get("name"))).append(" = ").append(tmp).append("[").append(String.valueOf(i)).append("];\n");
                }
            }
            case "exprCmd" -> {
                emitExpr((AstNode) cmd.get("expr"));
            }
        }
    }

    private String emitExpr(AstNode e){
        switch (String.valueOf(e.get("type"))){
            case "int" -> { return String.valueOf(e.get("value")); }
            case "float" -> { return String.valueOf(e.get("value")); }
            case "bool" -> { return String.valueOf(e.get("value")); }
            case "char" -> { return "\'"+String.valueOf(e.get("value"))+"\'"; }
            case "string" -> { return '"'+String.valueOf(e.get("value"))+'"'; }
            case "null" -> { return "null"; }
            case "var" -> { return String.valueOf(e.get("name")); }
            case "un" -> {
                String op = String.valueOf(e.get("op"));
                String a = emitExpr((AstNode) e.get("expr"));
                if (op.equals("!")) return "(!toBool("+a+"))";
                if (op.equals("-")) return "(-toNum("+a+"))";
                if (op.equals("+")) return "(toNum("+a+"))";
                return a;
            }
            case "bin" -> {
                String op = String.valueOf(e.get("op"));
                String l = emitExpr((AstNode) e.get("left"));
                String r = emitExpr((AstNode) e.get("right"));
                if (op.equals("&&")) return "(toBool("+l+") && toBool("+r+"))";
                if (op.equals("||")) return "(toBool("+l+") || toBool("+r+"))";
                if (Set.of("==","!=","<",">","<=",">=").contains(op)) return "(compare("+l+","+r+")"+cmpToJava(op)+")";
                return "(toNum("+l+") "+op+" toNum("+r+"))";
            }
            case "arrayAccess" -> {
                String a = emitExpr((AstNode) e.get("array"));
                String i = emitExpr((AstNode) e.get("index"));
                return "((Object[])("+a+"))[toInt("+i+")]";
            }
            case "fieldAccess" -> {
                String o = emitExpr((AstNode) e.get("obj"));
                return "((java.util.HashMap)("+o+")).get(\""+String.valueOf(e.get("field"))+"\")";
            }
            case "arrayLit" -> {
                @SuppressWarnings("unchecked") List<Object> els = (List<Object>) e.get("elements");
                String tmp = fresh();
                out.append("    Object[] ").append(tmp).append(" = new Object[").append(String.valueOf(els==null?0:els.size())).append("];\n");
                if (els!=null) for (int i=0;i<els.size();i++) out.append("    ").append(tmp).append("[").append(String.valueOf(i)).append("] = ").append(emitExpr((AstNode) els.get(i))).append(";\n");
                return tmp;
            }
            case "newRec" -> {
                String tmp = fresh();
                out.append("    java.util.HashMap ").append(tmp).append(" = new java.util.HashMap();\n");
                return tmp;
            }
            case "newArr" -> {
                String n = emitExpr((AstNode) e.get("size"));
                String tmp = fresh();
                out.append("    Object[] ").append(tmp).append(" = new Object[toInt(").append(n).append(")];\n");
                return tmp;
            }
            case "callExpr" -> {
                AstNode fn = (AstNode) e.get("func");
                @SuppressWarnings("unchecked") List<Object> as = (List<Object>) e.get("args");
                StringBuilder sb = new StringBuilder();
                sb.append(String.valueOf(fn.get("name"))).append("(");
                if (as!=null) for (int i=0;i<as.size();i++){ if (i>0) sb.append(", "); sb.append(emitExpr((AstNode) as.get(i))); }
                sb.append(")");
                return sb.toString();
            }
        }
        return "null";
    }

    private String fresh(){ return "__t"+(++tempCounter); }

    private AstNode lastStep(AstNode lv){
        AstNode cur=lv; AstNode nx=(AstNode)cur.get("next"); if (nx==null) return null; while (nx.get("next")!=null) nx=(AstNode)nx.get("next"); return nx;
    }

    private static boolean toBool(Object o){
        if (o instanceof Boolean b) return b;
        if (o instanceof Number n) return n.doubleValue()!=0.0;
        if (o instanceof Character c) return c.charValue()!=0;
        if (o instanceof String s) return !s.isEmpty();
        if (o instanceof Collection<?> c) return !c.isEmpty();
        if (o instanceof Map<?,?> m) return !m.isEmpty();
        if (o instanceof Object[] a) return a.length!=0;
        return o!=null;
    }
    private static double toNum(Object o){
        if (o instanceof Integer i) return i;
        if (o instanceof Double d) return d;
        if (o instanceof Float f) return f.doubleValue();
        if (o instanceof Long l) return l.doubleValue();
        if (o instanceof Short s) return s.doubleValue();
        if (o instanceof Byte b) return b.doubleValue();
        if (o instanceof Character c) return (int)c.charValue();
        if (o instanceof Boolean b) return b?1:0;
        return 0;
    }
    private static int toInt(Object o){ return (int) Math.floor(toNum(o)); }
    private static Iterable<Object> toIterable(Object o){
        if (o instanceof Object[] a) return Arrays.asList(a);
        if (o instanceof Collection<?> c) return (Iterable<Object>) c;
        return List.of();
    }
    private static String cmpToJava(String op){
        return switch (op){
            case "==" -> "==0"; case "!=" -> "!=0"; case "<" -> "<0"; case ">" -> ">0"; case "<=" -> "<=0"; case ">=" -> ">=0"; default -> "==0"; };
    }
    private static int compare(Object a, Object b){
        double x = toNum(a), y = toNum(b);
        return Double.compare(x,y);
    }
}


