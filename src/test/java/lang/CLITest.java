package lang;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.*;

public class CLITest {
    @TempDir
    Path tempDir;
    
    private Path createTestFile(String content) throws IOException {
        Path file = tempDir.resolve("test.lang");
        Files.write(file, content.getBytes());
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
                CLI.main(new String[]{"-syn", file.toString()});
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
                CLI.main(new String[]{"-syn", file.toString()});
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
                CLI.main(new String[]{"-i", file.toString()});
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
                CLI.main(new String[]{"-i", file.toString()});
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
        run("data R{ n: Int; }  main(){ r=new R; r.n=7; print r.n }", "7\n");
    }

    private void run(String code, String expectedOutput) throws Exception {
        // Criar arquivo temporário
        Path tempFile = Files.createTempFile("test", ".lang");
        Files.write(tempFile, code.getBytes());
        
        try {
            // Executar CLI
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(baos));
            
            CLI.main(new String[]{"-i", tempFile.toString()});
            
            System.setOut(originalOut);
            String output = baos.toString();
            
            // Normalizar quebras de linha (CRLF -> LF)
            String normalizedOutput = output.replace("\r\n", "\n");
            String normalizedExpected = expectedOutput.replace("\r\n", "\n");
            
            // Comparar saída ignorando espaços/quebras de linha finais
            assertEquals(normalizedExpected.trim(), normalizedOutput.trim());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
} 