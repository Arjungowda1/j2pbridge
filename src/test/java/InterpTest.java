import org.arjun.interpreter.JavaLexer;
import org.arjun.interpreter.JavaParser;
import org.arjun.interpreter._interpreter_;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class InterpTest {

    @Test
    public void interpreterTest(){
        // Load Java code from resources
        String javaCode;
        try {
            java.nio.file.Path resourcePath = java.nio.file.Paths.get(getClass().getResource("/samples/Exception0.java").toURI());

            javaCode = new String(Files.readAllBytes(resourcePath), StandardCharsets.UTF_8);
            
            // Process the Java code directly
            org.antlr.v4.runtime.CharStream input = org.antlr.v4.runtime.CharStreams.fromString(javaCode);
            JavaLexer lexer = new JavaLexer(input);
            org.antlr.v4.runtime.CommonTokenStream tokens = new org.antlr.v4.runtime.CommonTokenStream(lexer);
            JavaParser parser = new JavaParser(tokens);
            JavaParser.CompilationUnitContext tree = parser.compilationUnit();
            
            // Create visitor and visit the parse tree
            _interpreter_ visitor = new _interpreter_();
            String actualPython = visitor.visit(tree);

            // Expected Python output
            String expectedPython = "x = 10\n" +
                                  "y = 20\n" +
                                  "sum = x + y\n" +
                                  "print(f\"Sum is: {sum}\")";
            
            // Normalize whitespace and remove empty lines for comparison
            actualPython = actualPython.trim().replaceAll("\\s+", " ");
            String normalizedExpected = expectedPython.trim().replaceAll("\\s+", " ");
            
            org.junit.jupiter.api.Assertions.assertEquals(normalizedExpected, actualPython);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to load or process Java file", e);
        }
    }
}
