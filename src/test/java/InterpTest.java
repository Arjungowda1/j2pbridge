import jdk.nashorn.internal.ir.annotations.Ignore;
import org.arjun.interpreter.JavaLexer;
import org.arjun.interpreter.JavaParser;
import org.arjun.interpreter._interpreter_;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@Ignore
public class InterpTest {

    @Test
    public void interpreterTest(){
        String javaCode;
        try {
            java.nio.file.Path resourcePath = java.nio.file.Paths.get(getClass().getResource("/samples/BasicType0.java").toURI());

            javaCode = new String(Files.readAllBytes(resourcePath), StandardCharsets.UTF_8);

            org.antlr.v4.runtime.CharStream input = org.antlr.v4.runtime.CharStreams.fromString(javaCode);
            JavaLexer lexer = new JavaLexer(input);
            org.antlr.v4.runtime.CommonTokenStream tokens = new org.antlr.v4.runtime.CommonTokenStream(lexer);
            JavaParser parser = new JavaParser(tokens);
            JavaParser.CompilationUnitContext tree = parser.compilationUnit();

            _interpreter_ visitor = new _interpreter_();
            String actualPython = visitor.visit(tree);

            String expectedPython = "class BasicTypes0:\n" +
                    "    def __init__(self):\n" +
                    "        pass\n" +
                    "\n" +
                    "    @staticmethod\n" +
                    "    def main(args: []):\n" +
                    "        B = 127\n" +
                    "        print(B)\n" +
                    "        S = 2934\n" +
                    "        print(S)\n" +
                    "        C = 'F'\n" +
                    "        print(C)\n" +
                    "        I = 48\n" +
                    "        print(I)\n" +
                    "        str = \"Whats up?\"\n" +
                    "        print(str)\n" +
                    "        L = 1234567890\n" +
                    "        print(L)\n" +
                    "        F = 0.1\n" +
                    "        print(F)\n" +
                    "        D = 0.1\n" +
                    "        print(D)\n" +
                    "        O = True\n" +
                    "        print((42 if O else -3))\n" +
                    "\n" +
                    "if __name__ == \"__main__\":\n" +
                    "    BasicTypes0.main([])";
            
            // Normalize whitespace and remove empty lines for comparison
            actualPython = actualPython.trim().replaceAll("\\s+", " ");
            String normalizedExpected = expectedPython.trim().replaceAll("\\s+", " ");
            
            org.junit.jupiter.api.Assertions.assertEquals(normalizedExpected, actualPython);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to load or process Java file", e);
        }
    }
}
