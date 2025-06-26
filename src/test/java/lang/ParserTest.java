package lang;

import lang.parser.*;
import org.antlr.v4.runtime.*;
import org.junit.jupiter.api.Test;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

public class ParserTest {

    @Test
    void acceptsValid() throws Exception {
        InputStream in = getClass().getResourceAsStream("/progs/ok.lang");
        CharStream cs = CharStreams.fromStream(in);
        LangLexer lex = new LangLexer(cs);
        CommonTokenStream ts = new CommonTokenStream(lex);
        LangParser p = new LangParser(ts);
        p.prog();
        assertEquals(0, p.getNumberOfSyntaxErrors());
    }
} 