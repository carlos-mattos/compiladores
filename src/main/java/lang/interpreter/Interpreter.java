package lang.interpreter;

import lang.ast.AstNode;
import java.util.*;

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
                @SuppressWarnings("unchecked")
                List<Object> commands = (List<Object>) cmd.get("commands");
                for (Object command : commands) {
                    if (command instanceof AstNode node) {
                        interpretCmd(node);
                    }
                }
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
                while (true) {
                    Value condition = interpretExpr((AstNode) cmd.get("condition"));
                    if (!isTrue(condition)) break;
                    interpretCmd((AstNode) cmd.get("body"));
                }
            }
            case "print" -> {
                Value value = interpretExpr((AstNode) cmd.get("expr"));
                System.out.println(valueToString(value));
            }
            case "read" -> {
                Scanner scanner = new Scanner(System.in);
                if (scanner.hasNextInt()) {
                    int value = scanner.nextInt();
                    AstNode expr = (AstNode) cmd.get("expr");
                    if ("var".equals(expr.get("type"))) {
                        String varName = (String) expr.get("name");
                        scopes.peek().put(varName, Value.intV(value));
                    }
                }
            }
            case "return" -> {
                @SuppressWarnings("unchecked")
                List<Object> exprs = (List<Object>) cmd.get("exprs");
                Value value = (exprs != null && !exprs.isEmpty()) ? interpretExpr((AstNode) exprs.get(0)) : Value.nullV();
                throw new ReturnException(value);
            }
            case "assign" -> {
                String varName = (String) cmd.get("var");
                Value value = interpretExpr((AstNode) cmd.get("expr"));
                scopes.peek().put(varName, value);
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
                    }
                    scopes.pop();
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
                        Value result = Value.nullV();
                        try {
                            interpretCmd(function.body);
                        } catch (ReturnException e) {
                            result = e.value;
                        }
                        scopes.pop();
                        yield result;
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
            default -> Value.nullV();
        };
    }
    
    private static class ReturnException extends RuntimeException {
        public final Value value;
        public ReturnException(Value value) {
            this.value = value;
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
} 