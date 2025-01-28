# j2pbridge

A Java to Python code conversion library that enables seamless transformation of Java source code into equivalent Python code.

## Overview

j2pbridge is a sophisticated library that bridges the gap between Java and Python ecosystems by providing automated code conversion capabilities. This tool is designed to help developers migrate Java codebases to Python while maintaining code functionality and structure.

## Features

- Converts Java syntax to Python syntax
- Preserves most of the code structure and logic
- Handles Java-specific constructs and translates them to Python equivalents
- Supports basic Java implementation and their Python counterparts
- Maintains code readability in the converted output

## Technical Implementation

### ANTLR (ANother Tool for Language Recognition)

This project leverages ANTLR4, a powerful parser generator that creates parsers from grammatical descriptions. ANTLR reads a formal grammar definition and generates source code that can parse text conforming to that grammar. In j2pbridge, ANTLR is used to:

- Parse Java source code into an Abstract Syntax Tree (AST)
- Provide a structured representation of the code that can be traversed and analyzed
- Generate visitor classes that walk through the AST

### Custom Visitor Pattern

I have implemented a custom visitor pattern using ANTLR's generated visitor interface to:

- Traverse the Java AST node by node
- Convert Java-specific constructs into their Python equivalents
- Handle context-sensitive transformations
- Generate equivalent Python code while preserving the original code structure

The visitor pattern allows me to separate the algorithm from the object structure it operates on, making the code conversion process modular and maintainable.

## Installation

1. ANTLR - For ANTLR4 installation and setup instructions, please refer to the [official ANTLR4 repository](https://github.com/antlr/antlr4). The repository contains comprehensive documentation.
2. Grammar used here can be found here: [JavaGrammar](https://github.com/antlr/grammars-v4/tree/master/java/java8)
3. This application runs on Java8

## Usage

The tool can be run from the command line using the following syntax:
1. Converting a file:
```bash
java Main -inputPath ./src/Example.java -outputPath ./output/Example.py
```
2. Converting a code snippet:
```bash
java Main -snippet "System.out.println('Hello World');" -outputPath ./output/snippet.py
```

Note: You must provide either an input file path or a code snippet, if both are provided only file is considered as input.

### Parameters

- `-inputPath`: Path to the Java source file to be converted
- `-snippet`: Direct Java code snippet to convert (as a string)
- `-outputPath`: Destination path for the converted Python code

## Known Limitations
- import statements are not handled
- python code may not work all the time
- only "Exception" class is handled

## Acknowledgments
Internet :)