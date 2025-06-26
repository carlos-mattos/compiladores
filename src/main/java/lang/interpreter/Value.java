package lang.interpreter;

import java.util.List;
import java.util.Map;

public class Value {
    public enum Type { INT, FLOAT, BOOL, CHAR, NULL, ARRAY, RECORD }
    
    private final Type type;
    private final Object value;
    
    private Value(Type type, Object value) {
        this.type = type;
        this.value = value;
    }
    
    public static Value intV(int value) { return new Value(Type.INT, value); }
    public static Value floatV(double value) { return new Value(Type.FLOAT, value); }
    public static Value boolV(boolean value) { return new Value(Type.BOOL, value); }
    public static Value charV(char value) { return new Value(Type.CHAR, value); }
    public static Value nullV() { return new Value(Type.NULL, null); }
    public static Value arrayV(List<Value> elems) { return new Value(Type.ARRAY, elems); }
    public static Value recordV(Map<String, Value> fields) { return new Value(Type.RECORD, fields); }
    
    public Type getType() { return type; }
    public Object getValue() { return value; }
    
    public int asInt() { return (Integer) value; }
    public double asFloat() { return (Double) value; }
    public boolean asBool() { return (Boolean) value; }
    public char asChar() { return (Character) value; }
    public List<Value> asArray() { return (List<Value>) value; }
    public Map<String, Value> asRecord() { return (Map<String, Value>) value; }
} 