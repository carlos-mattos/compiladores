package lang;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

public class RegressionTest {

    @Test
    void fullRegressionSuite() throws Exception {
        Path prog = Path.of(
            getClass()
                .getResource("/regression/all.lang")
                .toURI()
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream prevOut = System.out;
        System.setOut(new PrintStream(out));

        CLI.main(new String[]{"-i", prog.toString()});

        System.setOut(prevOut);

        String expected = """
                3
                2
                1
                4
                5
                9
                7
                1
                1
                """;

        // normaliza CRLF vs LF
        String actual = out.toString().replace("\r\n", "\n").trim();
        assertEquals(expected.trim(), actual);
    }
} 