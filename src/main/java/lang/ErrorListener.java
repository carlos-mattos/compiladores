/*
 * allan amaral - 201935001
 * carlos mattos - 201935003
 */

package lang;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import java.util.concurrent.atomic.AtomicInteger;

public class ErrorListener extends BaseErrorListener {
    private final AtomicInteger syntaxErrors = new AtomicInteger(0);
    
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, 
                           int line, int charPositionInLine, String msg, 
                           RecognitionException e) {
        System.err.println("SYNTAX ERROR: " + msg + " at line " + line + ":" + charPositionInLine);
        syntaxErrors.incrementAndGet();
    }
    
    public int getSyntaxErrors() {
        return syntaxErrors.get();
    }
    
    public void reset() {
        syntaxErrors.set(0);
    }
} 