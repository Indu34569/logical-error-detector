# Static Analysis Tool for Detecting Logical Errors in Java Programs

A Java-based static analysis tool that analyzes source code and detects selected logical errors using program analysis, constraint generation, and the Z3 SMT solver.

## Project Overview

The tool accepts Java source code through a web-based interface and analyzes the program to identify logical errors.

The system provides:

- Java code input through a web interface
- JavaParser-based source code analysis
- Variable and condition analysis
- Constraint generation
- Z3-based logical verification
- Error detection with line numbers
- Multiple error reporting
- Web-based analysis results

## Analysis Pipeline

Java Code Input  
↓  
JavaParser – AST Generation  
↓  
Data Flow – Variable Analysis  
↓  
Constraints – Constraint Generation  
↓  
Z3 Solver – Logical Verification  
↓  
Report – Error Detection

## Technologies Used

- Java 17
- Maven
- JavaParser
- Z3 SMT Solver
- Java HTTP Server
- HTML
- CSS
- JavaScript
- GitHub
- GitHub Pages
- Render

## Live Demo

https://indu34569.github.io/logical-error-detector/

## Backend

https://logical-error-detector-backend.onrender.com/

## Source Code

https://github.com/Indu34569/logical-error-detector

## Example

### Input

```java
public class TestProgram {
    public static void main(String[] args) {
        int age = 20;

        if (age < 10) {
            System.out.println("Child");
        }
    }
}