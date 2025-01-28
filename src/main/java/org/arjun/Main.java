package org.arjun;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.arjun.interpreter.JavaLexer;
import org.arjun.interpreter.JavaParser;
import org.arjun.interpreter._interpreter_;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception{
        if (args.length < 4) {
            System.out.println("Usage: java Main -inputPath <inputFile> -snippet <javaSnippet> -outputPath <outputFile>");
            return;
        }

        String inputPath = null;
        String snippet = null;
        String outputPath = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-inputPath":
                    inputPath = args[++i];
                    break;
                case "-snippet":
                    snippet = args[++i];
                    break;
                case "-outputPath":
                    outputPath = args[++i];
                    break;
                default:
                    System.out.println("Unknown argument: " + args[i]);
                    return;
            }
        }
        processJavaCode(inputPath, snippet, outputPath);
    }

    private static void processJavaCode(String inputPath, String snippet, String outputPath) throws IOException {
        try {
            CharStream codeCharStream;
            if (inputPath != null) {
                codeCharStream = CharStreams.fromFileName(inputPath);
            } else if (snippet != null) {
                codeCharStream = CharStreams.fromString(snippet);
            } else {
                System.out.println("Error: Either inputPath or snippet must be provided.");
                return;
            }
            JavaLexer lexer = new JavaLexer(codeCharStream);
            CommonTokenStream tokenStream = new CommonTokenStream(lexer);
            JavaParser parser = new JavaParser(tokenStream);
            final _interpreter_ interpreter = new _interpreter_();
            String pythonCode = interpreter.visit(parser.compilationUnit());
            writeToFile(outputPath, pythonCode);
        } catch (Exception exception) {
            String prefix = LocalDate.now() + "interpreter_logs";
            File logFile = Files.createTempFile(prefix,".log").toFile();
            writeToFile(logFile.getPath(), Arrays.toString(exception.getStackTrace()));
            System.out.println("Exception occurred! Unable to interpret the java snippet. Find logs at: "+logFile.toPath());
        }
    }

    private static void writeToFile(String outputPath, String content) {
        try (FileWriter writer = new FileWriter(outputPath)) {
            writer.write(content);
            System.out.println("Python code written to: " + outputPath);
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }
}
