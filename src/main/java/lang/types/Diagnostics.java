/*
 * allan amaral - 201935001
 * carlos mattos - 201935003
 */

package lang.types;

import java.util.*;

public class Diagnostics {
  private final List<Diagnostic> list = new ArrayList<>();
  
  public void error(String msg) { 
    list.add(new Diagnostic(msg)); 
  }
  
  public boolean ok() { 
    return list.isEmpty(); 
  }
  
  public List<Diagnostic> all() { 
    return List.copyOf(list); 
  }
}
