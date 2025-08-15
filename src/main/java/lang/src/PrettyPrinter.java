/*
 * allan amaral - 201935001
 * carlos mattos - 201935003
 */

package lang.src;

import lang.ast.AstNode;
import java.util.*;

public class PrettyPrinter {
  private final StringBuilder sb = new StringBuilder();
  private int ind=0; 
  
  private void nl(){ 
    sb.append("\n"); 
    for(int i=0;i<ind;i++) sb.append("  "); 
  }
  
  public String print(AstNode prog){
    @SuppressWarnings("unchecked") List<Object> defs = (List<Object>) prog.get("definitions");
    for (int i = 0; i < defs.size(); i++) {
      Object d = defs.get(i);
      if (d instanceof AstNode n) {
        printDef(n);
        if (i < defs.size() - 1) {
          nl();
        }
      }
    }
    return sb.toString().trim();
  }
  
  private void printDef(AstNode n){
    switch ((String)n.get("type")){
      case "dataDecl" -> { 
        if (Boolean.TRUE.equals(n.get("isAbstract"))) sb.append("abstract ");
        sb.append("data ").append(n.get("name"));
        sb.append(" {");
        @SuppressWarnings("unchecked") List<Object> members = (List<Object>) n.get("members");
        if (members != null) {
          for (Object m : members) {
            if (m instanceof AstNode dm) {
              Object memberTypeField = dm.get("type");
              if (memberTypeField instanceof AstNode) {
                nl(); ind++;
                sb.append(dm.get("name")).append(" :: ");
                printType((AstNode) memberTypeField);
                sb.append(";");
                ind--;
              } else if ("funDecl".equals(memberTypeField)) {
                nl(); ind++;
                printDef(dm);
                ind--;
              }
            }
          }
        }
        nl(); sb.append("}");
      }
      case "funDecl" -> { 
        sb.append(n.get("name")).append("(");
        @SuppressWarnings("unchecked") List<Object> ps = (List<Object>) n.get("params");
        if (ps != null) {
                     for (int i = 0; i < ps.size(); i++) {
             if (i > 0) sb.append(", ");
             AstNode p = (AstNode) ps.get(i);
             sb.append(p.get("name")).append(" :: ");
             printType((AstNode) p.get("type"));
           }
        }
        sb.append(")");
        
        @SuppressWarnings("unchecked") List<Object> rt = (List<Object>) n.get("returnTypes");
        if (rt != null && !rt.isEmpty()) {
          sb.append(" : ");
                     for (int i = 0; i < rt.size(); i++) {
             if (i > 0) sb.append(", ");
             AstNode t = (AstNode) rt.get(i);
             printType(t);
           }
        }
        
        sb.append(" {");
        if (n.get("body") != null) {
          nl(); ind++;
          printCmd((AstNode) n.get("body"));
          ind--;
        }
        nl(); sb.append("}");
      }
    }
  }
  
  private void printCmd(AstNode cmd) {
    switch ((String)cmd.get("type")) {
      case "block" -> {
        @SuppressWarnings("unchecked") List<Object> cs = (List<Object>) cmd.get("commands");
        if (cs != null) {
          for (Object c : cs) {
            if (c instanceof AstNode n) {
              nl(); printCmd(n);
            }
          }
        }
      }
      case "if" -> {
        sb.append("if "); 
        printExpr((AstNode) cmd.get("condition"));
        sb.append(" then {");
        if (cmd.get("then") != null) {
          nl(); 
          ind++; 
          printCmd((AstNode) cmd.get("then")); 
          ind--;
        }
        nl(); 
        sb.append("}");
        if (cmd.get("else") != null) {
          nl(); 
          sb.append("else {");
          if (cmd.get("else") != null) {
            nl(); 
            ind++; 
            printCmd((AstNode) cmd.get("else")); 
            ind--;
          }
          nl(); 
          sb.append("}");
        }
      }
      case "iterate" -> {
        sb.append("iterate(");
        if (cmd.get("id") != null) {
          printLValue((AstNode) cmd.get("id"));
          sb.append(": ");
          printExpr((AstNode) cmd.get("expr"));
        } else {
          printExpr((AstNode) cmd.get("expr"));
        }
        sb.append(") {");
        if (cmd.get("body") != null) {
          nl(); ind++; printCmd((AstNode) cmd.get("body")); ind--;
        }
        nl(); sb.append("}");
      }
      case "print" -> {
        sb.append("print "); 
        printExpr((AstNode) cmd.get("expr")); 
        sb.append(';');
      }
      case "read" -> {
        sb.append("read "); 
        printLValue((AstNode) cmd.get("lvalue")); 
        sb.append(';');
      }
       case "return" -> {
         sb.append("return");
         @SuppressWarnings("unchecked") List<Object> es = (List<Object>) cmd.get("exprs");
         if (es != null && !es.isEmpty()) {
           sb.append(" ");
           for (int i = 0; i < es.size(); i++) {
             if (i > 0) sb.append(", ");
             printExpr((AstNode) es.get(i));
           }
         }
         sb.append(';');
       }
      case "assign" -> {
        printLValue((AstNode) cmd.get("lvalue"));
        sb.append(" = ");
        printExpr((AstNode) cmd.get("expr"));
        sb.append(';');
      }
      case "call" -> {
        sb.append(cmd.get("func")).append("(");
        @SuppressWarnings("unchecked") List<Object> as = (List<Object>) cmd.get("args");
        if (as != null) {
          for (int i = 0; i < as.size(); i++) {
            if (i > 0) sb.append(", ");
            printExpr((AstNode) as.get(i));
          }
        }
        sb.append(")");
        sb.append(';');
      }
       case "callWithRet" -> {
         sb.append(cmd.get("func")).append("(");
         @SuppressWarnings("unchecked") List<Object> as = (List<Object>) cmd.get("args");
         if (as != null) {
           for (int i = 0; i < as.size(); i++) {
             if (i > 0) sb.append(", ");
             printExpr((AstNode) as.get(i));
           }
         }
         sb.append(") < ");
         @SuppressWarnings("unchecked") List<Object> lvs = (List<Object>) cmd.get("lvalues");
         if (lvs != null) {
           for (int i = 0; i < lvs.size(); i++) {
             if (i > 0) sb.append(", ");
             printLValue((AstNode) lvs.get(i));
           }
         }
         sb.append(" >");
         sb.append(';');
       }
      case "exprCmd" -> {
        printExpr((AstNode) cmd.get("expr"));
        sb.append(';');
      }
    }
  }
  
  private void printExpr(AstNode e) {
    switch (extractTypeName(e.get("type"))) {
      case "int" -> sb.append(e.get("value"));
      case "float" -> sb.append(e.get("value"));
      case "bool" -> sb.append(e.get("value"));
      case "char" -> sb.append("'").append(e.get("value")).append("'");
      case "string" -> sb.append("\"").append(e.get("value")).append("\"");
      case "null" -> sb.append("null");
      case "var" -> sb.append(e.get("name"));
      case "un" -> {
        sb.append(e.get("op")); printExpr((AstNode) e.get("expr"));
      }
      case "bin" -> {
        printExpr((AstNode) e.get("left"));
        sb.append(" ").append(e.get("op")).append(" ");
        printExpr((AstNode) e.get("right"));
      }
      case "arrayAccess" -> {
        printExpr((AstNode) e.get("array"));
        sb.append("["); printExpr((AstNode) e.get("index")); sb.append("]");
      }
      case "fieldAccess" -> {
        printExpr((AstNode) e.get("obj"));
        sb.append(".").append(e.get("field"));
      }
      case "arrayLit" -> {
        sb.append("[");
        @SuppressWarnings("unchecked") List<Object> els = (List<Object>) e.get("elements");
        if (els != null) {
          for (int i = 0; i < els.size(); i++) {
            if (i > 0) sb.append(", ");
            printExpr((AstNode) els.get(i));
          }
        }
        sb.append("]");
      }
      case "newRec" -> {
        sb.append("new "); printType((AstNode) e.get("recType"));
      }
      case "newArr" -> {
        sb.append("new "); printType((AstNode) e.get("elemType"));
        sb.append("["); printExpr((AstNode) e.get("size")); sb.append("]");
      }
      case "callExpr" -> {
        printExpr((AstNode) e.get("func"));
        sb.append("(");
        @SuppressWarnings("unchecked") List<Object> as = (List<Object>) e.get("args");
        if (as != null) {
          for (int i = 0; i < as.size(); i++) {
            if (i > 0) sb.append(", ");
            printExpr((AstNode) as.get(i));
          }
        }
        sb.append(")");
      }
    }
  }
  
  private void printType(AstNode t) {
    switch (extractTypeName(t.get("type"))) {
      case "intType" -> sb.append("Int");
      case "floatType" -> sb.append("Float");
      case "boolType" -> sb.append("Bool");
      case "charType" -> sb.append("Char");
      case "arrayType" -> {
        printType((AstNode) t.get("elementType"));
        sb.append("[]");
      }
      case "productType" -> {
        sb.append("(");
        printType((AstNode) t.get("left"));
        sb.append(", ");
        printType((AstNode) t.get("right"));
        sb.append(")");
      }
      case "simpleType" -> sb.append(t.get("name"));
      case "type" -> sb.append(t.get("name"));
    }
  }
  
  private void printLValue(AstNode lv) {
    sb.append(lv.get("name"));
    AstNode cur = lv;
    while (cur.get("next") != null) {
      AstNode step = (AstNode) cur.get("next");
      switch (extractTypeName(step.get("type"))) {
        case "arrayAccess" -> { sb.append("["); printExpr((AstNode) step.get("index")); sb.append("]"); }
        case "fieldAccess" -> { sb.append(".").append(step.get("field")); }
      }
      cur = step;
    }
  }
  
  private static String extractTypeName(Object o){
    if (o instanceof String s) return s;
    if (o instanceof AstNode n) return (String) n.get("name");
    return String.valueOf(o);
  }
}
