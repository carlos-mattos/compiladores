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
        System.out.println("[testBlockCommentNested output]:\n" + output);
        assertTrue(output.contains("reject"));
    }

    @Test
    void testTruthinessBool() throws Exception {
        Path file = createTestFile("main() { if 1 then print \"should fail\" }");
        System.out.println("[testTruthinessBool file content]:\n" + Files.readString(file));
        final StringBuilder excMsg = new StringBuilder();
        String output = captureOutput(() -> {
            try {
                CLI.setTestMode(true);
                CLI.run(new String[]{"-i", file.toString()});
            } catch (Exception e) {
                excMsg.append("[Exception]: " + e + "\n");
            }
        });
        System.out.println("[testTruthinessBool output]:\n" + output);
        if (!excMsg.isEmpty()) System.out.println(excMsg);
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
} 