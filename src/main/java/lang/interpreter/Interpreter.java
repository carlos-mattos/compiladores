package lang.interpreter;

import lang.ast.AstNode;
import java.util.*;
import java.util.Collections;

public class Interpreter {
    private final Map<String, Value> variables = new HashMap<>();
    private final Map<String, FunctionDef> functions = new HashMap<>();
    private final Deque<Map<String, Value>> scopes = new ArrayDeque<>();
    
    public static class FunctionDef {
        public final List<String> params;
        public final AstNode body;
        
        public FunctionDef(List<String> params, AstNode body) {
            this.params = params;
            this.body = body;
        }
    }
    
    public Interpreter() {
        scopes.push(new HashMap<>());
    }
    
    public void interpret(AstNode ast) {
        if ("prog".equals(ast.get("type"))) {
            @SuppressWarnings("unchecked")
            List<Object> definitions = (List<Object>) ast.get("definitions");
            
            for (Object def : definitions) {
                if (def instanceof AstNode node && "funDecl".equals(node.get("type"))) {
                    String name = (String) node.get("name");
                    @SuppressWarnings("unchecked")
                    List<Object> params = (List<Object>) node.get("params");
                    List<String> paramNames = new ArrayList<>();
                    for (Object param : params) {
                        if (param instanceof AstNode paramNode) {
                            paramNames.add((String) paramNode.get("name"));
                        }
                    }
                    Object body = node.get("body");
                    if (body instanceof AstNode bodyNode) {
                        functions.put(name, new FunctionDef(paramNames, bodyNode));
                    }
                }
            }
            
            FunctionDef main = functions.get("main");
            if (main == null) {
                System.err.println("No main function");
                System.exit(2);
            }
            if (!main.params.isEmpty()) {
                System.err.println("No main function");
                System.exit(2);
            }
            
            interpretCmd(main.body);
        }
    }
    
    private void interpretCmd(AstNode cmd) {
        String type = (String) cmd.get("type");
        
        switch (type) {
            case "block" -> {
                scopes.push(new HashMap<>());
                @SuppressWarnings("unchecked")
                List<Object> commands = (List<Object>) cmd.get("commands");
                for (Object command : commands) {
                    if (command instanceof AstNode node) {
                        interpretCmd(node);
                    }
                }
                scopes.pop();
            }
            case "if" -> {
                Value condition = interpretExpr((AstNode) cmd.get("condition"));
                if (isTrue(condition)) {
                    interpretCmd((AstNode) cmd.get("then"));
                } else {
                    interpretCmd((AstNode) cmd.get("else"));
                }
            }
            case "iterate" -> {
                if (cmd.get("id") != null) {       // forma com variável
                    AstNode idLv = (AstNode) cmd.get("id");
                    Value base = interpretExpr((AstNode) cmd.get("expr"));
                    List<Value> elems;
                    if (base.getType() == Value.Type.INT) {
                        int n = base.asInt();
                        elems = new ArrayList<>();
                        for (int k = n; k >= 1; k--) elems.add(Value.intV(k));   // ordem decrescente
                    } else if (base.getType() == Value.Type.ARRAY) {
                        elems = base.asArray();
                    } else {
                        throw new RuntimeException("iterate expects int or array");
                    }
                    for (Value v : elems) {
                        assign(idLv, v);     // agora aceita lvalue composto
                        interpretCmd((AstNode) cmd.get("body"));
                    }
                } else {                            // iterate(expr)
                    int n = interpretExpr((AstNode) cmd.get("expr")).asInt();
                    for (int k = 0; k < n; k++) interpretCmd((AstNode) cmd.get("body"));
                }
            }
            case "print" -> {
                Value value = interpretExpr((AstNode) cmd.get("expr"));
                System.out.println(valueToString(value));
            }
            case "read" -> {
                AstNode lvalue = (AstNode) cmd.get("lvalue");
                Scanner scanner = new Scanner(System.in);
                if (scanner.hasNextInt()) {
                    int value = scanner.nextInt();
                    assign(lvalue, Value.intV(value));
                }
            }
            case "return" -> {
                @SuppressWarnings("unchecked")
                List<Object> exprs = (List<Object>) cmd.get("exprs");
                List<Value> values = new ArrayList<>();
                
                if (exprs != null && !exprs.isEmpty()) {
                    for (Object expr : exprs) {
                        if (expr instanceof AstNode node) {
                            values.add(interpretExpr(node));
                        }
                    }
                } else {
                    values.add(Value.nullV());
                }
                
                throw new ReturnException(values);
            }
            case "assign" -> {
                AstNode lvalue = (AstNode) cmd.get("lvalue");
                Value value = interpretExpr((AstNode) cmd.get("expr"));
                assign(lvalue, value);
            }
            case "call" -> {
                String funcName = (String) cmd.get("func");
                @SuppressWarnings("unchecked")
                List<Object> args = (List<Object>) cmd.get("args");
                
                FunctionDef func = functions.get(funcName);
                if (func != null) {
                    Map<String, Value> newScope = new HashMap<>();
                    scopes.push(newScope);
                    for (int i = 0; i < func.params.size() && i < args.size(); i++) {
                        String paramName = func.params.get(i);
                        Value argValue = interpretExpr((AstNode) args.get(i));
                        newScope.put(paramName, argValue);
                    }
                    try {
                        interpretCmd(func.body);
                    } catch (ReturnException e) {
                        // Ignorar retornos de chamadas simples
                    }
                    scopes.pop();
                }
            }
            case "callWithRet" -> {
                String funcName = (String) cmd.get("func");
                @SuppressWarnings("unchecked")
                List<Object> args = (List<Object>) cmd.get("args");
                @SuppressWarnings("unchecked")
                List<Object> lvalues = (List<Object>) cmd.get("lvalues");
                
                FunctionDef func = functions.get(funcName);
                if (func != null) {
                    Map<String, Value> newScope = new HashMap<>();
                    scopes.push(newScope);
                    for (int i = 0; i < func.params.size() && i < args.size(); i++) {
                        String paramName = func.params.get(i);
                        Value argValue = interpretExpr((AstNode) args.get(i));
                        newScope.put(paramName, argValue);
                    }
                    
                    List<Value> returnValues = new ArrayList<>();
                    try {
                        interpretCmd(func.body);
                    } catch (ReturnException e) {
                        returnValues = e.values;
                    }
                    scopes.pop();
                    
                    // Atribuir os valores retornados aos lvalues
                    for (int i = 0; i < lvalues.size() && i < returnValues.size(); i++) {
                        AstNode lvalue = (AstNode) lvalues.get(i);
                        Value value = returnValues.get(i);
                        assign(lvalue, value);
                    }
                }
            }
            case "exprCmd" -> interpretExpr((AstNode) cmd.get("expr"));
        }
    }
    
    private Value interpretExpr(AstNode expr) {
        String type = (String) expr.get("type");
        
        return switch (type) {
            case "int" -> Value.intV((Integer) expr.get("value"));
            case "float" -> Value.floatV((Double) expr.get("value"));
            case "bool" -> Value.boolV((Boolean) expr.get("value"));
            case "char" -> Value.charV((Character) expr.get("value"));
            case "var" -> {
                String name = (String) expr.get("name");
                Value value = null;
                for (Map<String, Value> scope : scopes) {
                    if (scope.containsKey(name)) {
                        value = scope.get(name);
                        break;
                    }
                }
                yield value != null ? value : Value.nullV();
            }
            case "bin" -> {
                Value left = interpretExpr((AstNode) expr.get("left"));
                Value right = interpretExpr((AstNode) expr.get("right"));
                String op = (String) expr.get("op");
                yield evaluateBinOp(left, right, op);
            }
            case "un" -> {
                Value operand = interpretExpr((AstNode) expr.get("expr"));
                String op = (String) expr.get("op");
                yield evaluateUnOp(operand, op);
            }
            case "callExpr" -> {
                Object func = expr.get("func");
                @SuppressWarnings("unchecked")
                List<Object> args = (List<Object>) expr.get("args");
                if (func instanceof AstNode funcNode && "var".equals(funcNode.get("type"))) {
                    String funcName = (String) funcNode.get("name");
                    FunctionDef function = functions.get(funcName);
                    if (function != null) {
                        Map<String, Value> newScope = new HashMap<>();
                        scopes.push(newScope);
                        for (int i = 0; i < function.params.size() && i < args.size(); i++) {
                            String paramName = function.params.get(i);
                            Value argValue = interpretExpr((AstNode) args.get(i));
                            newScope.put(paramName, argValue);
                        }
                        Value result = Value.arrayV(new ArrayList<>()); // lista vazia por padrão
                        try {
                            interpretCmd(function.body);
                        } catch (ReturnException e) {
                            result = Value.arrayV(e.values);   // devolve sempre lista
                        }
                        scopes.pop();
                        yield result;
                    }
                }
                yield Value.nullV();
            }
            case "select" -> {
                // Seleção [idx] de valor retornado
                Value base = interpretExpr((AstNode) expr.get("base"));
                Value index = interpretExpr((AstNode) expr.get("index"));
                
                if (base.getType() == Value.Type.ARRAY && index.getType() == Value.Type.INT) {
                    List<Value> elems = base.asArray();
                    int idx = index.asInt();
                    if (idx >= 0 && idx < elems.size()) {
                        yield elems.get(idx);
                    }
                }
                yield Value.nullV();
            }
            case "arrayAccess" -> {
                Value array = interpretExpr((AstNode) expr.get("array"));
                Value index = interpretExpr((AstNode) expr.get("index"));
                if (array.getType() == Value.Type.ARRAY && index.getType() == Value.Type.INT) {
                    List<Value> elems = array.asArray();
                    int idx = index.asInt();
                    if (idx >= 0 && idx < elems.size()) {
                        yield elems.get(idx);
                    }
                }
                yield Value.nullV();
            }
            case "fieldAccess" -> {
                Value record = interpretExpr((AstNode) expr.get("obj"));
                String field = (String) expr.get("field");
                if (record.getType() == Value.Type.RECORD) {
                    Map<String, Value> fields = record.asRecord();
                    if (fields.containsKey(field)) {
                        yield fields.get(field);
                    }
                }
                yield Value.nullV();
            }
            case "newRec" -> Value.recordV(new HashMap<>()); // campos default = null
            case "newArr" -> {
                int n = interpretExpr((AstNode) expr.get("size")).asInt();
                List<Value> list = new ArrayList<>(Collections.nCopies(n, Value.nullV()));
                yield Value.arrayV(list);
            }
            case "arrayLit" -> {
                @SuppressWarnings("unchecked")
                List<Object> elements = (List<Object>) expr.get("elements");
                List<Value> values = new ArrayList<>();
                for (Object element : elements) {
                    values.add(interpretExpr((AstNode) element));
                }
                yield Value.arrayV(values);
            }
            default -> Value.nullV();
        };
    }
    
    private static class ReturnException extends RuntimeException {
        public final List<Value> values;
        public ReturnException(List<Value> values) {
            this.values = values;
        }
        public ReturnException(Value value) {
            this.values = List.of(value);
        }
    }
    
    private Value evaluateBinOp(Value left, Value right, String op) {
        return switch (op) {
            case "+" -> add(left, right);
            case "-" -> subtract(left, right);
            case "*" -> multiply(left, right);
            case "/" -> divide(left, right);
            case "%" -> modulo(left, right);
            case "==" -> Value.boolV(equals(left, right));
            case "!=" -> Value.boolV(!equals(left, right));
            case "<" -> Value.boolV(lessThan(left, right));
            case ">" -> Value.boolV(greaterThan(left, right));
            case "<=" -> Value.boolV(lessThanOrEqual(left, right));
            case ">=" -> Value.boolV(greaterThanOrEqual(left, right));
            case "&&" -> Value.boolV(isTrue(left) && isTrue(right));
            case "||" -> Value.boolV(isTrue(left) || isTrue(right));
            default -> Value.nullV();
        };
    }
    
    private Value evaluateUnOp(Value operand, String op) {
        return switch (op) {
            case "+" -> operand;
            case "-" -> negate(operand);
            case "!" -> Value.boolV(!isTrue(operand));
            default -> Value.nullV();
        };
    }
    
    private Value add(Value left, Value right) {
        if (left.getType() == Value.Type.INT && right.getType() == Value.Type.INT) {
            return Value.intV(left.asInt() + right.asInt());
        } else if (left.getType() == Value.Type.FLOAT && right.getType() == Value.Type.FLOAT) {
            return Value.floatV(left.asFloat() + right.asFloat());
        } else if (left.getType() == Value.Type.INT && right.getType() == Value.Type.FLOAT) {
            return Value.floatV(left.asInt() + right.asFloat());
        } else if (left.getType() == Value.Type.FLOAT && right.getType() == Value.Type.INT) {
            return Value.floatV(left.asFloat() + right.asInt());
        }
        return Value.nullV();
    }
    
    private Value subtract(Value left, Value right) {
        if (left.getType() == Value.Type.INT && right.getType() == Value.Type.INT) {
            return Value.intV(left.asInt() - right.asInt());
        } else if (left.getType() == Value.Type.FLOAT && right.getType() == Value.Type.FLOAT) {
            return Value.floatV(left.asFloat() - right.asFloat());
        } else if (left.getType() == Value.Type.INT && right.getType() == Value.Type.FLOAT) {
            return Value.floatV(left.asInt() - right.asFloat());
        } else if (left.getType() == Value.Type.FLOAT && right.getType() == Value.Type.INT) {
            return Value.floatV(left.asFloat() - right.asInt());
        }
        return Value.nullV();
    }
    
    private Value multiply(Value left, Value right) {
        if (left.getType() == Value.Type.INT && right.getType() == Value.Type.INT) {
            return Value.intV(left.asInt() * right.asInt());
        } else if (left.getType() == Value.Type.FLOAT && right.getType() == Value.Type.FLOAT) {
            return Value.floatV(left.asFloat() * right.asFloat());
        } else if (left.getType() == Value.Type.INT && right.getType() == Value.Type.FLOAT) {
            return Value.floatV(left.asInt() * right.asFloat());
        } else if (left.getType() == Value.Type.FLOAT && right.getType() == Value.Type.INT) {
            return Value.floatV(left.asFloat() * right.asInt());
        }
        return Value.nullV();
    }
    
    private Value divide(Value left, Value right) {
        if (left.getType() == Value.Type.INT && right.getType() == Value.Type.INT) {
            return right.asInt() != 0 ? Value.intV(left.asInt() / right.asInt()) : Value.nullV();
        } else if (left.getType() == Value.Type.FLOAT && right.getType() == Value.Type.FLOAT) {
            return right.asFloat() != 0.0 ? Value.floatV(left.asFloat() / right.asFloat()) : Value.nullV();
        } else if (left.getType() == Value.Type.INT && right.getType() == Value.Type.FLOAT) {
            return right.asFloat() != 0.0 ? Value.floatV(left.asInt() / right.asFloat()) : Value.nullV();
        } else if (left.getType() == Value.Type.FLOAT && right.getType() == Value.Type.INT) {
            return right.asInt() != 0 ? Value.floatV(left.asFloat() / right.asInt()) : Value.nullV();
        }
        return Value.nullV();
    }
    
    private Value modulo(Value left, Value right) {
        if (left.getType() == Value.Type.INT && right.getType() == Value.Type.INT) {
            return right.asInt() != 0 ? Value.intV(left.asInt() % right.asInt()) : Value.nullV();
        }
        return Value.nullV();
    }
    
    private Value negate(Value operand) {
        if (operand.getType() == Value.Type.INT) {
            return Value.intV(-operand.asInt());
        } else if (operand.getType() == Value.Type.FLOAT) {
            return Value.floatV(-operand.asFloat());
        }
        return Value.nullV();
    }
    
    private boolean equals(Value left, Value right) {
        if (left.getType() == Value.Type.INT && right.getType() == Value.Type.INT) {
            return left.asInt() == right.asInt();
        } else if (left.getType() == Value.Type.FLOAT && right.getType() == Value.Type.FLOAT) {
            return left.asFloat() == right.asFloat();
        } else if (left.getType() == Value.Type.BOOL && right.getType() == Value.Type.BOOL) {
            return left.asBool() == right.asBool();
        }
        return false;
    }
    
    private boolean lessThan(Value left, Value right) {
        if (left.getType() == Value.Type.INT && right.getType() == Value.Type.INT) {
            return left.asInt() < right.asInt();
        } else if (left.getType() == Value.Type.FLOAT && right.getType() == Value.Type.FLOAT) {
            return left.asFloat() < right.asFloat();
        }
        return false;
    }
    
    private boolean greaterThan(Value left, Value right) {
        if (left.getType() == Value.Type.INT && right.getType() == Value.Type.INT) {
            return left.asInt() > right.asInt();
        } else if (left.getType() == Value.Type.FLOAT && right.getType() == Value.Type.FLOAT) {
            return left.asFloat() > right.asFloat();
        }
        return false;
    }
    
    private boolean lessThanOrEqual(Value left, Value right) {
        if (left.getType() == Value.Type.INT && right.getType() == Value.Type.INT) {
            return left.asInt() <= right.asInt();
        } else if (left.getType() == Value.Type.FLOAT && right.getType() == Value.Type.FLOAT) {
            return left.asFloat() <= right.asFloat();
        }
        return false;
    }
    
    private boolean greaterThanOrEqual(Value left, Value right) {
        if (left.getType() == Value.Type.INT && right.getType() == Value.Type.INT) {
            return left.asInt() >= right.asInt();
        } else if (left.getType() == Value.Type.FLOAT && right.getType() == Value.Type.FLOAT) {
            return left.asFloat() >= right.asFloat();
        }
        return false;
    }
    
    private boolean isTrue(Value value) {
        if (value.getType() == Value.Type.BOOL) {
            return value.asBool();
        } else if (value.getType() == Value.Type.INT) {
            return value.asInt() != 0;
        } else if (value.getType() == Value.Type.FLOAT) {
            return value.asFloat() != 0.0;
        }
        return false;
    }
    
    private String valueToString(Value value) {
        if (value.getType() == Value.Type.INT) {
            return String.valueOf(value.asInt());
        } else if (value.getType() == Value.Type.FLOAT) {
            return String.valueOf(value.asFloat());
        } else if (value.getType() == Value.Type.BOOL) {
            return String.valueOf(value.asBool());
        } else if (value.getType() == Value.Type.CHAR) {
            return String.valueOf(value.asChar());
        } else if (value.getType() == Value.Type.NULL) {
            return "null";
        }
        return "unknown";
    }
    
    private Value interpretLvalue(AstNode lvalue) {
        if (!"lvalue".equals(lvalue.get("type"))) {
            return Value.nullV();
        }
        
        String name = (String) lvalue.get("name");
        Value value = null;
        
        // Procurar a variável nos escopos
        for (Map<String, Value> scope : scopes) {
            if (scope.containsKey(name)) {
                value = scope.get(name);
                break;
            }
        }
        
        if (value == null) {
            return Value.nullV();
        }
        
        // Processar acessos encadeados (array, campo)
        AstNode current = lvalue;
        while (current.get("next") != null) {
            AstNode next = (AstNode) current.get("next");
            String nextType = (String) next.get("type");
            
            if ("arrayAccess".equals(nextType)) {
                Value index = interpretExpr((AstNode) next.get("index"));
                if (value.getType() == Value.Type.ARRAY && index.getType() == Value.Type.INT) {
                    List<Value> elems = value.asArray();
                    int idx = index.asInt();
                    if (idx >= 0 && idx < elems.size()) {
                        value = elems.get(idx);
                    } else {
                        return Value.nullV();
                    }
                } else {
                    return Value.nullV();
                }
            } else if ("fieldAccess".equals(nextType)) {
                String field = (String) next.get("field");
                if (value.getType() == Value.Type.RECORD) {
                    Map<String, Value> fields = value.asRecord();
                    if (fields.containsKey(field)) {
                        value = fields.get(field);
                    } else {
                        return Value.nullV();
                    }
                } else {
                    return Value.nullV();
                }
            }
            
            current = next;
        }
        
        return value;
    }

    private Value lookupVar(String id) {
        for (var s : scopes) if (s.containsKey(id)) return s.get(id);
        return Value.nullV();
    }

    private Value deref(AstNode lv) { /* usa enquanto lê */
        if (!"lvalue".equals(lv.get("type"))) return Value.nullV();
        Value base = lookupVar((String) lv.get("name"));
        AstNode cur = lv;
        while (cur.get("next") != null) {
            AstNode nxt = (AstNode) cur.get("next");
            if ("arrayAccess".equals(nxt.get("type"))) {
                int i = interpretExpr((AstNode) nxt.get("index")).asInt();
                base = base.asArray().get(i);
            } else { // field
                String f = (String) nxt.get("field");
                base = base.asRecord().get(f);
            }
            cur = nxt;
        }
        return base;
    }

    private void assign(AstNode lv, Value v) {
        if (!"lvalue".equals(lv.get("type"))) throw new RuntimeException("not an lvalue");
        if (lv.get("next") == null) {
            scopes.peek().put((String) lv.get("name"), v);
        } else {
            Value base = lookupVar((String) lv.get("name"));
            AstNode cur = lv;
            AstNode prev = null;
            while (cur.get("next") != null) {
                prev = cur;
                cur = (AstNode) cur.get("next");
            }
            if ("arrayAccess".equals(cur.get("type"))) {
                int i = interpretExpr((AstNode) cur.get("index")).asInt();
                base.asArray().set(i, v);
            } else if ("fieldAccess".equals(cur.get("type"))) {
                String f = (String) cur.get("field");
                Map<String, Value> rec = base.asRecord();
                if (rec == null) {
                    // Se o record não foi inicializado, crie um novo
                    rec = new HashMap<>();
                    // Substitua o valor na variável original
                    scopes.peek().put((String) lv.get("name"), Value.recordV(rec));
                }
                rec.put(f, v);
            }
        }
    }
} 