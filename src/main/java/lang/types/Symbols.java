/*
 * allan amaral - 201935001
 * carlos mattos - 201935003
 */

package lang.types;

import java.util.*;

public class Symbols {
  public static final class FunSig {
    public final List<Type> params; public final List<Type> returns;
    public FunSig(List<Type> p, List<Type> r){ this.params=p; this.returns=r; }
  }
  private final Deque<Map<String,Type>> scopes = new ArrayDeque<>();
  private final Map<String,FunSig> funs = new HashMap<>();
  private final Map<String,Type.Record> records = new HashMap<>();

  public Symbols(){ push(); }
  public void push(){ scopes.push(new HashMap<>()); }
  public void pop(){ scopes.pop(); }
  public void putVar(String name, Type t){ scopes.peek().put(name,t); }
  public Optional<Type> lookupVar(String name){
    for (var s: scopes) if (s.containsKey(name)) return Optional.of(s.get(name));
    return Optional.empty();
  }
  public void putFun(String name, FunSig sig){ funs.put(name, sig); }
  public Optional<FunSig> getFun(String name){ return Optional.ofNullable(funs.get(name)); }
  public Map<String,FunSig> funSigsView(){ return Collections.unmodifiableMap(funs); }
  public void putRecord(Type.Record r){ records.put(r.name, r); }
  public Optional<Type.Record> getRecord(String name){ return Optional.ofNullable(records.get(name)); }
}
