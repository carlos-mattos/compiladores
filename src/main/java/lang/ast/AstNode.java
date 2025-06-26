package lang.ast;

import java.util.*;

public class AstNode extends HashMap<String, Object> {
    public AstNode(String type) {
        this.put("type", type);
    }
    public AstNode(String type, Object... kvs) {
        this(type);
        for (int i = 0; i < kvs.length; i += 2) {
            this.put((String) kvs[i], kvs[i + 1]);
        }
    }
} 