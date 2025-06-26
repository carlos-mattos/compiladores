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
} 