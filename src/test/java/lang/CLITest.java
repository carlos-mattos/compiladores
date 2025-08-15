/*
 * allan amaral - 201935001
 * carlos mattos - 201935003
 */

package lang;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

public class CLITest {
    @TempDir
    Path tempDir;
    
    private Path createTestFile(String content) throws IOException {
        Path file = tempDir.resolve("test.lang");
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        return file;
    }
    
    private String captureOutput(Runnable runnable) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        
        try {
            System.setOut(new PrintStream(out));
            System.setErr(new PrintStream(out));
            runnable.run();
            return out.toString();
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

    @Test
    void testSynAccept() throws Exception {
        Path file = createTestFile("main() { print 42 }");
        String output = captureOutput(() -> {
            try {
                CLI.setTestMode(true);
                CLI.run(new String[]{"-syn", file.toString()});
            } catch (Exception e) {
            }
        });
        assertTrue(output.contains("accept"));
    }

    @Test
    void testSynReject() throws Exception {
        Path file = createTestFile("main() { print }");
        String output = captureOutput(() -> {
            try {
                CLI.setTestMode(true);
                CLI.run(new String[]{"-syn", file.toString()});
            } catch (Exception e) {
            }
        });
        assertTrue(output.contains("reject"));
    }

    @Test
    void testInterpreter() throws Exception {
        Path file = createTestFile("main() { print 6 * 7 }");
        String output = captureOutput(() -> {
            try {
                CLI.setTestMode(true);
                CLI.run(new String[]{"-i", file.toString()});
            } catch (Exception e) {
            }
        });
        assertTrue(output.contains("42"));
    }

    @Test
    void testComplexInterpreter() throws Exception {
        Path file = createTestFile("main() { x = 10; y = 5; if x > y then print x + y else print x - y }");
        String output = captureOutput(() -> {
            try {
                CLI.setTestMode(true);
                CLI.run(new String[]{"-i", file.toString()});
            } catch (Exception e) {
            }
        });
        assertTrue(output.contains("15"));
    }

    @Test
    void iterateIdInt() throws Exception {
        run("""
            main(){ iterate(i:5){ print i } }
            """, "5\n4\n3\n2\n1");
    }

    @Test
    void iterateIdArray() throws Exception {
        run("""
            main(){ iterate(e:[1,2,3]){ print e } }
            """, "1\n2\n3");
    }

    @Test
    void iteratePrettyPrinterParens() throws Exception {
        Path file = createTestFile("main(){ iterate(i:[1,2]){ print i } }");
        String out = captureOutput(() -> {
            try {
                CLI.setTestMode(true);
                CLI.run(new String[]{"-src", file.toString()});
            } catch (Exception ignored) {}
        });
        String compact = out.replace("\r\n","\n").replaceAll("\\s+", " ");
        assertTrue(compact.contains("iterate(i: [1, 2])"));
    }

    @Test
    void prettyPrinterRoundTrip() throws Exception {
        String originalCode = """
            data R{ 
              n::Int; 
              m::Int;
              add(x::Int):Int { return n + x }
            }
            main(){ 
              r = new R;
              iterate(i:[1,2,3]){ print i }
            }
            """;
        
        Path file = createTestFile(originalCode);
        String prettyOutput = captureOutput(() -> {
            try {
                CLI.setTestMode(true);
                CLI.run(new String[]{"-src", file.toString()});
            } catch (Exception ignored) {}
        });
        
        Path prettyFile = createTestFile(prettyOutput);
        String parseResult = captureOutput(() -> {
            try {
                CLI.setTestMode(true);
                CLI.run(new String[]{"-syn", prettyFile.toString()});
            } catch (Exception ignored) {}
        });
        
        assertTrue(parseResult.contains("accept"), "PrettyPrinter output should be reparseable");
    }

    @Test
    void typeCheckerRejectsAbstractNew() throws Exception {
        Path file = createTestFile("abstract data R{} main(){ x = new R }");
        String output = captureOutput(() -> {
            try {
                CLI.setTestMode(true);
                CLI.run(new String[]{"-t", file.toString()});
            } catch (Exception e) { }
        });
        assertTrue(output.contains("reject"), "TypeChecker should reject new of abstract type");
    }

    @Test
    void typeCheckerRejectsInvalidArrayIndex() throws Exception {
        Path file = createTestFile("main(){ arr = [1,2,3]; x = arr[true] }");
        String output = captureOutput(() -> {
            try {
                CLI.setTestMode(true);
                CLI.run(new String[]{"-t", file.toString()});
            } catch (Exception e) { }
        });
        assertTrue(output.contains("reject"), "TypeChecker should reject boolean array index");
    }

    @Test
    void typeCheckerRejectsInvalidIfCondition() throws Exception {
        Path file = createTestFile("main(){ if 42 then print 1 }");
        String output = captureOutput(() -> {
            try {
                CLI.setTestMode(true);
                CLI.run(new String[]{"-t", file.toString()});
            } catch (Exception e) { }
        });
        assertTrue(output.contains("reject"), "TypeChecker should reject non-boolean if condition");
    }

    @Test
    void typecheckRejectsAbstractNew() throws Exception {
        Path file = createTestFile("abstract data R{} main(){ x=new R }");
        String output = captureOutput(() -> {
            try {
                CLI.setTestMode(true);
                CLI.run(new String[]{"-t", file.toString()});
            } catch (Exception e) { }
        });
        assertTrue(output.contains("reject"));
    }

    @Test
    void nestedAssignInterpreter() throws Exception {
        run("""
            data R{ a::Int[]; }
            main(){
              r = new R;
              r.a = [0,0,0];
              r.a[1] = 7;
              print r.a[1]
            }
            """, "7");
    }

    @Test
    void functionCallAndReturn() throws Exception {
        run("""
            add(a::Int, b::Int):Int { return a + b }
            main(){ print add(5, 3) }
            """, "8");
    }

    @Test
    void multipleReturnValues() throws Exception {
        run("""
            divmod(a::Int, b::Int):Int,Int { q = a / b; r = a % b; return q, r }
            main(){ 
              result = divmod(7, 3);
              print result[0]
            }
            """, "2");
    }

    @Test
    void recordFieldAccess() throws Exception {
        run("""
            data R{ n::Int; }
            main(){ 
              r = new R;
              r.n = 42;
              print r.n
            }
            """, "42");
    }

    @Test
    void shortCircuitEvaluation() throws Exception {
        run("""
            main(){ 
              x = 0;
              if (x != 0) && (1 / x > 0) then print 1;
              print 0
            }
            """, "0");
    }

    @Test
    void arrayOperations() throws Exception {
        run("""
            main(){ 
              arr = [1, 2, 3];
              arr[1] = 99;
              print arr[1]
            }
            """, "99");
    }

    @Test
    void iterateWithArray() throws Exception {
        run("""
            main(){ 
              iterate(i:[5, 10, 15]){ print i }
            }
            """, "5\n10\n15");
    }

    @Test
    void tupleSelection() throws Exception {
        run("""
            divmod(a::Int,b::Int):Int,Int{ q=a/b; r=a%b; return q,r }
            main(){ print divmod(7,3)[1] }
            """, "1");
    }

    @Test
    void newAndField() throws Exception {
        run("data R{ n:: Int; }  main(){ r=new R; r.n=7; print r.n }", "7\n");
    }

    private void run(String code, String expectedOutput) throws Exception {
        Path tempFile = Files.createTempFile("test", ".lang");
        Files.write(tempFile, code.getBytes());
        
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(baos));
            
            CLI.setTestMode(true);
            CLI.run(new String[]{"-i", tempFile.toString()});
            
            System.setOut(originalOut);
            String output = baos.toString();
            
            String normalizedOutput = output.replace("\r\n", "\n");
            String normalizedExpected = expectedOutput.replace("\r\n", "\n");
            
            assertEquals(normalizedExpected.trim(), normalizedOutput.trim());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void testCharEscapes() throws Exception {
        run("main() { print '\\n'; print '\\t'; print '\\'' }", "\n\t'");
    }

    @Test
    void testIfNoElse() throws Exception {
        run("main() { x = 10; if x > 5 then print x; print 42 }", "10\n42");
    }

    @Test
    void testShortCircuitAnd() throws Exception {
        run("main() { a = 0; if (a != 0) && (a == 1) then print 2; print 3 }", "3");
    }

    @Test
    void testShortCircuitOr() throws Exception {
        run("main() { a = 1; if (a != 0) || (a == 0) then print 2; print 3 }", "2\n3");
    }

    @Test
    void testShortCircuitSideEffects() throws Exception {
        run("side(n::Int):Bool{ print n; return false } main(){ if false && side(42) then print 1; print 2 }", "2");
    }

    @Test
    void testDivisionByZero() throws Exception {
        Path file = createTestFile("main() { x = 1 / 0 }");
        String output = captureOutput(() -> {
            try {
                CLI.setTestMode(true);
                CLI.run(new String[]{"-i", file.toString()});
            } catch (Exception e) {
            }
        });
        assertTrue(output.contains("runtime error"));
    }

    @Test
    void testArrayOutOfBounds() throws Exception {
        Path file = createTestFile("main() { v = [1, 2, 3]; print v[5] }");
        String output = captureOutput(() -> {
            try {
                CLI.setTestMode(true);
                CLI.run(new String[]{"-i", file.toString()});
            } catch (Exception e) {
            }
        });
        assertTrue(output.contains("runtime error"));
    }

    @Test
    void testScopeShadowing() throws Exception {
        run("main() { x = 1; { x = 2; } print x }", "2");
    }

    @Test
    void testBlockCommentNested() throws Exception {
        Path file = createTestFile("main() { {- {- aninhado -} -} print 42 }");
        String output = captureOutput(() -> {
            try {
                CLI.setTestMode(true);
                CLI.run(new String[]{"-syn", file.toString()});
            } catch (Exception e) {
            }
        });
        assertTrue(output.contains("accept"));
    }

    @Test
    void testTruthinessBool() throws Exception {
        Path file = createTestFile("main() { if 1 then print \"should fail\" }");
        String output = captureOutput(() -> {
            try {
                CLI.setTestMode(true);
                CLI.run(new String[]{"-i", file.toString()});
            } catch (Exception e) {}
        });
        assertTrue(output.contains("runtime error"));
    }

    @Test
    void testArrayPrint() throws Exception {
        run("main() { v = [1, 2, 3]; print v }", "[1,2,3]");
    }

    @Test
    void testExitCodes() throws Exception {
        Path file = createTestFile("main() { print 42 }");
        String output = captureOutput(() -> {
            try {
                CLI.setTestMode(true);
                CLI.run(new String[]{"-syn", file.toString()});
            } catch (Exception e) {
            }
        });
        assertTrue(output.contains("accept"));
    }

    @Test
    void testTypecheckRejectsTypeError() throws Exception {
        Path file = createTestFile("main() { if 1 then print 42 }");
        String output = captureOutput(() -> {
            try {
                CLI.setTestMode(true);
                CLI.run(new String[]{"-t", file.toString()});
            } catch (Exception e) {
            }
        });
        assertTrue(output.contains("reject"));
    }

    @Test
    void testInterpreterRuntimeError() throws Exception {
        Path file = createTestFile("main() { if 1 then print 42 }");
        String output = captureOutput(() -> {
            try {
                CLI.setTestMode(true);
                CLI.run(new String[]{"-i", file.toString()});
            } catch (Exception e) {}
        });
        assertTrue(output.contains("runtime error"));
    }

    @Test
    void typecheckIterateDeclaresId() throws Exception {
        Path file = createTestFile("main(){ iterate(i:[1,2]){ print i } }");
        String out = captureOutput(() -> {
            try { CLI.setTestMode(true); CLI.run(new String[]{"-t", file.toString()}); } catch (Exception ignored) {}
        });
        assertTrue(out.contains("accept"), "iterate(i: ...) deveria aceitar e declarar i");
    }

    @Test
    void typecheckFirstAssignmentDeclaresVar() throws Exception {
        Path file = createTestFile("main(){ x=[1,2,3]; print x[0] }");
        String out = captureOutput(() -> {
            try { CLI.setTestMode(true); CLI.run(new String[]{"-t", file.toString()}); } catch (Exception ignored) {}
        });
        assertTrue(out.contains("accept"), "primeira atribuição deveria declarar x como Array<Int>");
    }

    @Test
    void prettyIterateMatchesGrammar() throws Exception {
        Path file = createTestFile("main(){ iterate(i:[1,2]){ print i } }");
        String out = captureOutput(() -> {
            try { CLI.setTestMode(true); CLI.run(new String[]{"-src", file.toString()}); } catch (Exception ignored) {}
        });
        String norm = out.replace("\r\n","\n");
        String compact = norm.replaceAll("\\s+", " ");
        assertTrue(compact.contains("iterate(i: [1, 2])"), "PrettyPrinter deve usar iterate(<lv>: <expr>) com ou sem espaços");
    }

    @Test
    void prettyCallCmdRoundTrip() throws Exception {
        String program = """
            foo(x::Int) { print x }
            main(){ foo(1) }
            """;
        Path file = createTestFile(program);
        String pretty = captureOutput(() -> {
            try { CLI.setTestMode(true); CLI.run(new String[]{"-src", file.toString()}); } catch (Exception ignored) {}
        });
        String compact = pretty.replace("\r\n","\n").replaceAll("\\s+", " ");
        assertTrue(compact.contains("foo(1)"), "PrettyPrinter deve imprimir chamadas como comando sem 'call'");

        Path prettyFile = createTestFile(pretty);
        String parseResult = captureOutput(() -> {
            try { CLI.setTestMode(true); CLI.run(new String[]{"-syn", prettyFile.toString()}); } catch (Exception ignored) {}
        });
        assertTrue(parseResult.contains("accept"));
    }

    @Test
    void prettyCallWithRetRoundTrip() throws Exception {
        String program = """
            pair(a::Int,b::Int):Int,Int { return a,b }
            main(){ pair(1,2) < x, y >; print x; print y }
            """;
        Path file = createTestFile(program);
        String pretty = captureOutput(() -> {
            try { CLI.setTestMode(true); CLI.run(new String[]{"-src", file.toString()}); } catch (Exception ignored) {}
        });
        String compact = pretty.replace("\r\n","\n").replaceAll("\\s+", " ");
        assertTrue(compact.contains("pair(1, 2) < x, y >"), "PrettyPrinter deve imprimir f(args) < lvalues > para callWithRet");

        Path prettyFile = createTestFile(pretty);
        String parseResult = captureOutput(() -> {
            try { CLI.setTestMode(true); CLI.run(new String[]{"-syn", prettyFile.toString()}); } catch (Exception ignored) {}
        });
        assertTrue(parseResult.contains("accept"));
    }

    @Test
    void rejectsMissingReturn() throws Exception {
        Path p = createTestFile("f():Int { if true then { } } main(){ }");
        String out = captureOutput(() -> { CLI.setTestMode(true); CLI.run(new String[]{"-t", p.toString()}); });
        assertTrue(out.contains("reject"));
    }

    @Test
    void rejectsMainWithParams() throws Exception {
        Path p = createTestFile("main(x::Int) { }");
        String out = captureOutput(() -> { CLI.setTestMode(true); CLI.run(new String[]{"-t", p.toString()}); });
        assertTrue(out.contains("reject"));
    }

    @Test
    void prettySrcRoundTripWithIfIterateAndCallWithRet() throws Exception {
        String program = """
            pair(a::Int,b::Int):Int,Int { return a,b }
            main(){ if true then { iterate(i:[1,2]){ pair(i,i) < x, y >; print x } } else { print 0 } }
            """;
        Path file = createTestFile(program);
        String pretty = captureOutput(() -> {
            try { CLI.setTestMode(true); CLI.run(new String[]{"-src", file.toString()}); } catch (Exception ignored) {}
        });
        Path prettyFile = createTestFile(pretty);
        String parseResult = captureOutput(() -> {
            try { CLI.setTestMode(true); CLI.run(new String[]{"-syn", prettyFile.toString()}); } catch (Exception ignored) {}
        });
        assertTrue(parseResult.contains("accept"));
    }

    @Test
    void iterateWithoutIdTypecheckAndPretty() throws Exception {
        String program = "main(){ iterate(3){ print 1 } }";
        Path file = createTestFile(program);

        String typeOut = captureOutput(() -> {
            try { CLI.setTestMode(true); CLI.run(new String[]{"-t", file.toString()}); } catch (Exception ignored) {}
        });
        assertTrue(typeOut.contains("accept"), "TypeChecker deve aceitar iterate(N) sem id");

        String pretty = captureOutput(() -> {
            try { CLI.setTestMode(true); CLI.run(new String[]{"-src", file.toString()}); } catch (Exception ignored) {}
        });
        String compact = pretty.replace("\r\n","\n").replaceAll("\\s+", " ");
        assertTrue(compact.contains("iterate(3) {"), "PrettyPrinter deve imprimir iterate(3) { ... }");
    }

    @Test
    void callWithRetArityMismatchRejectsOnTypecheck() throws Exception {
        String program = """
            f(a::Int):Int,Int { return a, a }
            main(){ f(1) < x > }
            """;
        Path file = createTestFile(program);
        String out = captureOutput(() -> {
            try { CLI.setTestMode(true); CLI.run(new String[]{"-t", file.toString()}); } catch (Exception ignored) {}
        });
        assertTrue(out.contains("reject"), "TypeChecker deve rejeitar callWithRet com aridade de retorno errada");
    }

  @Test
  void typecheckMissingReturnInSomePath() throws Exception {
      Path f = createTestFile("f(x::Int):Int { if x>0 then return x } main(){}");
      String out = captureOutput(() -> {
          try { CLI.setTestMode(true); CLI.run(new String[]{"-t", f.toString()}); } catch (Exception ignored) {}
      });
      assertTrue(out.contains("reject"));
  }

  @Test
  void typecheckAllPathsReturnOk() throws Exception {
      Path f = createTestFile("f(x::Int):Int { if x>0 then return x else return -x } main(){}");
      String out = captureOutput(() -> {
          try { CLI.setTestMode(true); CLI.run(new String[]{"-t", f.toString()}); } catch (Exception ignored) {}
      });
      assertTrue(out.contains("accept"));
  }

  @Test
  void typecheckMainNoParams() throws Exception {
      Path f = createTestFile("main(a::Int){}");
      String out = captureOutput(() -> {
          try { CLI.setTestMode(true); CLI.run(new String[]{"-t", f.toString()}); } catch (Exception ignored) {}
      });
      assertTrue(out.contains("reject"));
  }

  @Test
  void typecheckMainMustBeVoid() throws Exception {
      Path f = createTestFile("main():Int { return 0 }");
      String out = captureOutput(() -> {
          try { CLI.setTestMode(true); CLI.run(new String[]{"-t", f.toString()}); } catch (Exception ignored) {}
      });
      assertTrue(out.contains("reject"));
  }
} 