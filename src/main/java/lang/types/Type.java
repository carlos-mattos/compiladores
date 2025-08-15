/*
 * allan amaral - 201935001
 * carlos mattos - 201935003
 */

package lang.types;

import java.util.*;

public sealed interface Type permits Type.Prim, Type.Array, Type.Product, Type.Record, Type.NullT, Type.StringT {
  enum Prim implements Type { INT, FLOAT, BOOL, CHAR }
  final class StringT implements Type { public static final StringT INSTANCE = new StringT(); private StringT(){} }
  final class NullT implements Type { public static final NullT INSTANCE = new NullT(); private NullT(){} }
  final class Array implements Type { public final Type elem; public Array(Type e){ this.elem = e; } }
  final class Product implements Type { public final List<Type> elems; public Product(List<Type> e){ this.elems = e; } }
  final class Record implements Type {
    public final String name; public final Map<String,Type> fields;
    public Record(String n, Map<String,Type> f){ this.name=n; this.fields=Map.copyOf(f); }
  }
}
