package com.project.analyzer;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.WhileStmt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogicalErrorDetector {

    private static final String INPUT_FILE =
            "test-input/tests/WebInput.java";

    private static final Set<String> reportedErrors =
            new HashSet<>();

    private static final Map<String, Integer> variables =
            new HashMap<>();

    private static final Set<String> initializedVariables =
            new HashSet<>();

    public static void main(String[] args) {

        System.out.println("========================================");
        System.out.println("        STATIC ANALYSIS TOOL");
        System.out.println("========================================");
        System.out.println("File: " + INPUT_FILE);
        System.out.println();

        reportedErrors.clear();
        variables.clear();
        initializedVariables.clear();

        Path file = Path.of(INPUT_FILE);

        if (!Files.exists(file)) {
            System.out.println("ERROR: Input file not found.");
            System.out.println("Expected file: " + INPUT_FILE);
            return;
        }

        String source;

        try {
            source = Files.readString(file);
        } catch (IOException e) {
            System.out.println("ERROR: Unable to read input file.");
            System.out.println(e.getMessage());
            return;
        }

        boolean syntaxError =
                analyzeSyntax(source);

        if (!syntaxError) {
            analyzeLogicalErrors(source);
        }

        printFinalReport(syntaxError);
    }

    // =====================================================
    // SYNTAX ANALYSIS
    // =====================================================

    private static boolean analyzeSyntax(String source) {

        System.out.println("SYNTAX ANALYSIS");
        System.out.println("----------------------------------------");

        try {
            StaticJavaParser.parse(source);

            System.out.println("✓ No syntax errors found.");
            System.out.println();

            return false;

        } catch (ParseProblemException e) {

            System.out.println("✗ Syntax errors detected.");

            String message = e.getMessage();

            if (message != null) {
                System.out.println(message);
            }

            System.out.println();

            return true;
        }
    }

    // =====================================================
    // LOGICAL ANALYSIS
    // =====================================================

    private static void analyzeLogicalErrors(String source) {

        System.out.println("LOGICAL ANALYSIS");
        System.out.println("----------------------------------------");

        analyzeDataFlow(source);

        analyzeControlFlow(source);

        analyzeDivisionByZero(source);

        analyzeConstantConditions(source);

        analyzeInfiniteLoops(source);

        analyzeMissingLoopUpdates(source);

        analyzeUnreachableCode(source);

        analyzeUninitializedVariables(source);

        analyzeDeadAssignments(source);
    }

    // =====================================================
    // PROGRAM-ORDER DATA-FLOW ANALYSIS
    // =====================================================

    private static void analyzeDataFlow(String source) {

        System.out.println("PROGRAM-ORDER DATA-FLOW ANALYSIS");
        System.out.println("----------------------------------------");

        variables.clear();
        initializedVariables.clear();

        String[] lines = source.split("\\R", -1);

        Pattern declarationPattern =
                Pattern.compile(
                        "\\b(?:int|long|short|byte|double|float)\\s+"
                                + "([A-Za-z_$][A-Za-z0-9_$]*)"
                                + "\\s*(?:=\\s*(.*?))?\\s*;"
                );

        Pattern assignmentPattern =
                Pattern.compile(
                        "^\\s*([A-Za-z_$][A-Za-z0-9_$]*)"
                                + "\\s*=\\s*(.+?)\\s*;"
                );

        for (int i = 0; i < lines.length; i++) {

            String line = removeComment(lines[i]).trim();

            if (line.isEmpty()) {
                continue;
            }

            Matcher declarationMatcher =
                    declarationPattern.matcher(line);

            if (declarationMatcher.find()) {

                String variableName =
                        declarationMatcher.group(1);

                String initializer =
                        declarationMatcher.group(2);

                if (initializer != null) {

                    initializer = initializer.trim();

                    Integer value =
                            evaluateIntegerExpression(
                                    initializer
                            );

                    if (value != null) {

                        variables.put(
                                variableName,
                                value
                        );

                        initializedVariables.add(
                                variableName
                        );

                        System.out.println(
                                "Line " + (i + 1)
                                        + ": Variable "
                                        + variableName
                                        + " = "
                                        + value
                        );

                    } else {

                        initializedVariables.add(
                                variableName
                        );

                        System.out.println(
                                "Line " + (i + 1)
                                        + ": Variable "
                                        + variableName
                                        + " initialized with expression"
                        );
                    }

                } else {

                    System.out.println(
                            "Line " + (i + 1)
                                    + ": Variable "
                                    + variableName
                                    + " declared without initialization"
                    );
                }

                continue;
            }

            Matcher assignmentMatcher =
                    assignmentPattern.matcher(line);

            if (assignmentMatcher.matches()) {

                String variableName =
                        assignmentMatcher.group(1);

                String expression =
                        assignmentMatcher.group(2).trim();

                Integer value =
                        evaluateIntegerExpression(expression);

                if (value != null) {

                    variables.put(
                            variableName,
                            value
                    );

                    initializedVariables.add(
                            variableName
                    );

                    System.out.println(
                            "Line " + (i + 1)
                                    + ": Variable "
                                    + variableName
                                    + " updated to "
                                    + value
                    );

                } else {

                    initializedVariables.add(
                            variableName
                    );

                    variables.remove(variableName);
                }
            }
        }

        System.out.println();
    }

    // =====================================================
    // IF / ELSE / LOOP ANALYSIS
    // =====================================================

    private static void analyzeControlFlow(String source) {

        System.out.println("IF / ELSE / LOOP ANALYSIS");
        System.out.println("----------------------------------------");

        String[] lines = source.split("\\R", -1);

        Pattern ifPattern =
                Pattern.compile(
                        "\\bif\\s*\\(([^)]*)\\)"
                );

        Pattern whilePattern =
                Pattern.compile(
                        "\\bwhile\\s*\\(([^)]*)\\)"
                );

        Pattern forPattern =
                Pattern.compile(
                        "\\bfor\\s*\\(([^)]*)\\)"
                );

        for (int i = 0; i < lines.length; i++) {

            String line =
                    removeComment(lines[i]).trim();

            Matcher ifMatcher =
                    ifPattern.matcher(line);

           if (ifMatcher.find()) {

    		System.out.println(
           	  "IF at line "
                    + (i + 1)
                    + ": "
                    + ifMatcher.group(1).trim()
    	   );

    	   analyzeImpossibleIfCondition(
            i + 1,
            ifMatcher.group(1).trim()
   	 );
  	}

            Matcher whileMatcher =
                    whilePattern.matcher(line);

            if (whileMatcher.find()) {

                String condition =
                        whileMatcher.group(1).trim();

                System.out.println(
                        "WHILE at line "
                                + (i + 1)
                                + ": "
                                + condition
                );

                analyzeImpossibleLoopCondition(
                        i + 1,
                        condition
                );
            }

            Matcher forMatcher =
                    forPattern.matcher(line);

            if (forMatcher.find()) {

                System.out.println(
                        "FOR at line "
                                + (i + 1)
                                + ": "
                                + forMatcher.group(1).trim()
                );
            }
        }

        System.out.println();
    }// =====================================================
// IMPOSSIBLE IF CONDITION
// =====================================================

private static void analyzeImpossibleIfCondition(
        int lineNumber,
        String condition) {

    Boolean result =
            evaluateCondition(condition);

    if (result != null && !result) {

        reportLogicalError(
                lineNumber,
                "Impossible IF Condition",
                condition,
                "The IF condition is false for the known variable values."
        );
    }
    }

    // =====================================================
    // IMPOSSIBLE LOOP CONDITION
    // =====================================================

    private static void analyzeImpossibleLoopCondition(
            int lineNumber,
            String condition) {

        Boolean result =
                evaluateCondition(condition);

        if (result != null && !result) {

            reportLogicalError(
                    lineNumber,
                    "Impossible Loop Condition",
                    condition,
                    "The loop condition is false for the known variable values."
            );
        }
    }

    // =====================================================
    // CONDITION EVALUATION
    // =====================================================

    private static Boolean evaluateCondition(
            String condition) {

        Matcher matcher =
                Pattern.compile(
                        "^\\s*"
                                + "([A-Za-z_$][A-Za-z0-9_$]*|-?\\d+)"
                                + "\\s*"
                                + "(<=|>=|==|!=|<|>)"
                                + "\\s*"
                                + "([A-Za-z_$][A-Za-z0-9_$]*|-?\\d+)"
                                + "\\s*$"
                ).matcher(condition);

        if (!matcher.matches()) {
            return null;
        }

        Integer left =
                getIntegerValue(matcher.group(1));

        Integer right =
                getIntegerValue(matcher.group(3));

        if (left == null || right == null) {
            return null;
        }

        String operator =
                matcher.group(2);

        switch (operator) {

            case ">":
                return left > right;

            case ">=":
                return left >= right;

            case "<":
                return left < right;

            case "<=":
                return left <= right;

            case "==":
                return left.equals(right);

            case "!=":
                return !left.equals(right);

            default:
                return null;
        }
    }

    // =====================================================
    // DIVISION-BY-ZERO ANALYSIS
    // =====================================================

    private static void analyzeDivisionByZero(
            String source) {

        System.out.println("DIVISION-BY-ZERO ANALYSIS");
        System.out.println("----------------------------------------");

        String[] lines =
                source.split("\\R", -1);

        boolean found = false;

        Pattern divisionPattern =
                Pattern.compile(
                        "([A-Za-z_$][A-Za-z0-9_$]*|-?\\d+)"
                                + "\\s*/\\s*"
                                + "([A-Za-z_$][A-Za-z0-9_$]*|-?\\d+)"
                );

        for (int i = 0; i < lines.length; i++) {

            String line =
                    removeComment(lines[i]);

            Matcher matcher =
                    divisionPattern.matcher(line);

            while (matcher.find()) {

                String divisor =
                        matcher.group(2);

                Integer divisorValue =
                        getIntegerValue(divisor);

                if (divisorValue != null
                        && divisorValue == 0) {

                    found = true;

                    reportLogicalError(
                            i + 1,
                            "Division By Zero",
                            matcher.group(),
                            "Division by zero detected. "
                                    + "The divisor evaluates to 0."
                    );
                }
            }
        }

        if (!found) {

            System.out.println(
                    "✓ No division-by-zero errors found."
            );
        }

        System.out.println();
    }

    // =====================================================
    // CONSTANT CONDITION ANALYSIS
    // =====================================================

    private static void analyzeConstantConditions(
            String source) {

        System.out.println(
                "CONSTANT CONDITION ANALYSIS"
        );

        System.out.println(
                "----------------------------------------"
        );

        String[] lines =
                source.split("\\R", -1);

        boolean found = false;

        Pattern ifPattern =
                Pattern.compile(
                        "\\bif\\s*\\(([^)]*)\\)"
                );

        for (int i = 0; i < lines.length; i++) {

            String line =
                    removeComment(lines[i]).trim();

            Matcher matcher =
                    ifPattern.matcher(line);

            if (matcher.find()) {

                String condition =
                        matcher.group(1).trim();

                Boolean result =
                        evaluateConstantCondition(
                                condition
                        );

                if (result != null) {

                    found = true;

                    reportLogicalError(
                            i + 1,
                            "Constant Condition",
                            condition,
                            "The IF condition always evaluates to "
                                    + result
                                    + "."
                    );
                }
            }
        }

        if (!found) {

            System.out.println(
                    "✓ No constant conditions found."
            );
        }

        System.out.println();
    }

    private static Boolean evaluateConstantCondition(
            String condition) {

        Matcher matcher =
                Pattern.compile(
                        "(-?\\d+)"
                                + "\\s*"
                                + "(<=|>=|==|!=|<|>)"
                                + "\\s*"
                                + "(-?\\d+)"
                ).matcher(condition);

        if (!matcher.matches()) {
            return null;
        }

        int left =
                Integer.parseInt(
                        matcher.group(1)
                );

        int right =
                Integer.parseInt(
                        matcher.group(3)
                );

        String operator =
                matcher.group(2);

        switch (operator) {

            case ">":
                return left > right;

            case ">=":
                return left >= right;

            case "<":
                return left < right;

            case "<=":
                return left <= right;

            case "==":
                return left == right;

            case "!=":
                return left != right;

            default:
                return null;
        }
    }

    // =====================================================
    // INFINITE LOOP ANALYSIS
    // =====================================================

    private static void analyzeInfiniteLoops(
            String source) {

        System.out.println(
                "INFINITE LOOP ANALYSIS"
        );

        System.out.println(
                "----------------------------------------"
        );

        String[] lines =
                source.split("\\R", -1);

        boolean found = false;

        for (int i = 0; i < lines.length; i++) {

            String line =
                    removeComment(lines[i]).trim();

            if (line.matches(
                    ".*while\\s*\\(\\s*true\\s*\\).*"
            )) {

                found = true;

                reportLogicalError(
                        i + 1,
                        "Potential Infinite Loop",
                        "while(true)",
                        "The while loop condition is always true."
                );
            }

            if (line.matches(
                    ".*for\\s*\\(\\s*;\\s*;\\s*\\).*"
            )) {

                found = true;

                reportLogicalError(
                        i + 1,
                        "Potential Infinite Loop",
                        "for(;;)",
                        "The for loop has no terminating condition."
                );
            }
        }

        if (!found) {

            System.out.println(
                    "✓ No obvious infinite loops found."
            );
        }

        System.out.println();
    }

    // =====================================================
    // MISSING LOOP UPDATE ANALYSIS
    // =====================================================

    private static void analyzeMissingLoopUpdates(
            String source) {

        System.out.println(
                "MISSING LOOP UPDATE ANALYSIS"
        );

        System.out.println(
                "----------------------------------------"
        );

        String[] lines =
                source.split("\\R", -1);

        boolean found = false;

        Pattern whilePattern =
                Pattern.compile(
                        "\\bwhile\\s*\\(([^)]*)\\)"
                );

        for (int i = 0; i < lines.length; i++) {

            String line =
                    removeComment(lines[i]).trim();

            Matcher matcher =
                    whilePattern.matcher(line);

            if (!matcher.find()) {
                continue;
            }

            String condition =
                    matcher.group(1).trim();

            String loopVariable =
                    findVariableInCondition(
                            condition
                    );

            if (loopVariable == null) {
                continue;
            }

            int braceDepth = 0;
            boolean started = false;
            boolean updated = false;

            for (int j = i; j < lines.length; j++) {

                String loopLine =
                        removeComment(lines[j]);

                for (char c : loopLine.toCharArray()) {

                    if (c == '{') {
                        braceDepth++;
                        started = true;
                    }

                    if (c == '}') {
                        braceDepth--;
                    }
                }

                if (j > i) {

                    if (loopLine.matches(
                            ".*\\b"
                                    + Pattern.quote(loopVariable)
                                    + "\\s*(\\+\\+|--|\\+=|-=|=).*"
                    )) {

                        updated = true;
                    }
                }

                if (started && braceDepth <= 0) {
                    break;
                }
            }

            if (!updated) {

                found = true;

                reportLogicalError(
                        i + 1,
                        "Missing Loop Update",
                        condition,
                        "Loop variable '"
                                + loopVariable
                                + "' is not updated inside the loop. "
                                + "The loop may not terminate."
                );
            }
        }

        if (!found) {

            System.out.println(
                    "✓ No missing loop updates found."
            );
        }

        System.out.println();
    }

    // =====================================================
    // FIND VARIABLE IN CONDITION
    // =====================================================

    private static String findVariableInCondition(
            String condition) {

        Matcher matcher =
                Pattern.compile(
                        "\\b([A-Za-z_$][A-Za-z0-9_$]*)\\b"
                ).matcher(condition);

        while (matcher.find()) {

            String word =
                    matcher.group(1);

            if (!word.equals("true")
                    && !word.equals("false")) {

                return word;
            }
        }

        return null;
    }

    // =====================================================
    // UNREACHABLE CODE ANALYSIS
    // =====================================================

    private static void analyzeUnreachableCode(
            String source) {

        System.out.println(
                "UNREACHABLE CODE ANALYSIS"
        );

        System.out.println(
                "----------------------------------------"
        );

        String[] lines =
                source.split("\\R", -1);

        boolean found = false;

        boolean afterReturn = false;
        boolean afterBreak = false;
        boolean afterContinue = false;

        int braceDepth = 0;

        for (int i = 0; i < lines.length; i++) {

            String line =
                    removeComment(lines[i]).trim();

            if (line.isEmpty()) {
                continue;
            }

            if (afterReturn
                    || afterBreak
                    || afterContinue) {

                if (!line.equals("}")
                        && !line.startsWith("}")) {

                    found = true;

                    String reason =
                            afterReturn
                                    ? "return statement"
                                    : afterBreak
                                    ? "break statement"
                                    : "continue statement";

                    reportLogicalError(
                            i + 1,
                            "Unreachable Code",
                            line,
                            "This statement appears after a "
                                    + reason
                                    + " and may never execute."
                    );

                    afterReturn = false;
                    afterBreak = false;
                    afterContinue = false;
                }
            }

            if (line.matches(
                    ".*\\breturn\\b.*;"
            )) {

                afterReturn = true;
            }

            if (line.matches(
                    ".*\\bbreak\\s*;"
            )) {

                afterBreak = true;
            }

            if (line.matches(
                    ".*\\bcontinue\\s*;"
            )) {

                afterContinue = true;
            }

            for (char c : line.toCharArray()) {

                if (c == '{') {
                    braceDepth++;
                }

                if (c == '}') {
                    braceDepth--;

                    if (braceDepth <= 0) {

                        afterReturn = false;
                        afterBreak = false;
                        afterContinue = false;
                    }
                }
            }
        }

        if (!found) {

            System.out.println(
                    "✓ No obvious unreachable code found."
            );
        }

        System.out.println();
    }

    // =====================================================
    // UNINITIALIZED VARIABLE ANALYSIS
    // =====================================================

    private static void analyzeUninitializedVariables(
            String source) {

        System.out.println(
                "UNINITIALIZED VARIABLE ANALYSIS"
        );

        System.out.println(
                "----------------------------------------"
        );

        Set<String> declared =
                new HashSet<>();

        Set<String> initialized =
                new HashSet<>();

        String[] lines =
                source.split("\\R", -1);

        Pattern declarationPattern =
                Pattern.compile(
                        "\\b(?:int|long|short|byte|double|float|"
                                + "boolean|char|String)\\s+"
                                + "([A-Za-z_$][A-Za-z0-9_$]*)"
                                + "\\s*(=\\s*([^;]+))?\\s*;"
                );

        Pattern identifierPattern =
                Pattern.compile(
                        "\\b[A-Za-z_$][A-Za-z0-9_$]*\\b"
                );

        boolean found = false;

        Set<String> keywords =
                Set.of(
                        "int",
                        "long",
                        "short",
                        "byte",
                        "double",
                        "float",
                        "boolean",
                        "char",
                        "String",
                        "if",
                        "else",
                        "while",
                        "for",
                        "return",
                        "true",
                        "false",
                        "break",
                        "continue",
                        "new",
                        "System",
                        "out",
                        "println"
                );

        for (int i = 0; i < lines.length; i++) {

            String line =
                    removeComment(lines[i]).trim();

            if (line.isEmpty()) {
                continue;
            }

            Matcher declarationMatcher =
                    declarationPattern.matcher(line);

            while (declarationMatcher.find()) {

                String variableName =
                        declarationMatcher.group(1);

                declared.add(variableName);

                String initializer =
                        declarationMatcher.group(3);

                if (initializer != null) {
                    initialized.add(variableName);
                }
            }

            Matcher identifierMatcher =
                    identifierPattern.matcher(line);

            while (identifierMatcher.find()) {

                String identifier =
                        identifierMatcher.group();

                if (keywords.contains(identifier)) {
                    continue;
                }

                if (!declared.contains(identifier)) {
                    continue;
                }

                int declarationPosition =
                        line.indexOf(identifier);

                boolean declarationLine =
                        declarationMatcher.find(
                                declarationPosition
                        );

                if (declarationLine) {
                    continue;
                }

                if (!initialized.contains(identifier)) {

                    found = true;

                    reportLogicalError(
                            i + 1,
                            "Uninitialized Variable",
                            identifier,
                            "Variable '"
                                    + identifier
                                    + "' may be used before initialization."
                    );

                    initialized.add(identifier);
                }
            }

            Pattern assignmentPattern =
                    Pattern.compile(
                            "^\\s*([A-Za-z_$][A-Za-z0-9_$]*)"
                                    + "\\s*="
                    );

            Matcher assignmentMatcher =
                    assignmentPattern.matcher(line);

            if (assignmentMatcher.find()) {

                initialized.add(
                        assignmentMatcher.group(1)
                );
            }
        }

        if (!found) {

            System.out.println(
                    "✓ No variables used before initialization found."
            );
        }

        System.out.println();
    }

    // =====================================================
    // DEAD ASSIGNMENT ANALYSIS
    // =====================================================

    private static void analyzeDeadAssignments(
            String source) {

        System.out.println(
                "DEAD ASSIGNMENT ANALYSIS"
        );

        System.out.println(
                "----------------------------------------"
        );

        String[] lines =
                source.split("\\R", -1);

        boolean found = false;

        Map<String, Integer> lastAssignmentLine =
                new HashMap<>();

        Map<String, Boolean> usedAfterAssignment =
                new HashMap<>();

        Pattern assignmentPattern =
                Pattern.compile(
                        "^\\s*([A-Za-z_$][A-Za-z0-9_$]*)"
                                + "\\s*=\\s*(.+?);\\s*$"
                );

        Pattern identifierPattern =
                Pattern.compile(
                        "\\b[A-Za-z_$][A-Za-z0-9_$]*\\b"
                );

        for (int i = 0; i < lines.length; i++) {

            String line =
                    removeComment(lines[i]).trim();

            Matcher assignmentMatcher =
                    assignmentPattern.matcher(line);

            if (assignmentMatcher.matches()) {

                String variable =
                        assignmentMatcher.group(1);

                if (lastAssignmentLine.containsKey(
                        variable
                )) {

                    Integer previousLine =
                            lastAssignmentLine.get(
                                    variable
                            );

                    Boolean used =
                            usedAfterAssignment.get(
                                    variable
                            );

                    if (Boolean.FALSE.equals(used)) {

                        found = true;

                        reportLogicalError(
                                previousLine,
                                "Dead Assignment",
                                variable,
                                "The value assigned to '"
                                        + variable
                                        + "' is overwritten "
                                        + "before being used."
                        );
                    }
                }

                lastAssignmentLine.put(
                        variable,
                        i + 1
                );

                usedAfterAssignment.put(
                        variable,
                        false
                );

                continue;
            }

            Matcher identifierMatcher =
                    identifierPattern.matcher(line);

            while (identifierMatcher.find()) {

                String identifier =
                        identifierMatcher.group();

                if (lastAssignmentLine.containsKey(
                        identifier
                )) {

                    usedAfterAssignment.put(
                            identifier,
                            true
                    );
                }
            }
        }

        if (!found) {

            System.out.println(
                    "✓ No obvious dead assignments found."
            );
        }

        System.out.println();
    }

    // =====================================================
    // INTEGER EXPRESSION EVALUATION
    // =====================================================

    private static Integer evaluateIntegerExpression(
            String expression) {

        expression =
                expression.trim();

        if (expression.matches("-?\\d+")) {

            try {
                return Integer.parseInt(expression);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        if (variables.containsKey(expression)) {
            return variables.get(expression);
        }

        Matcher arithmetic =
                Pattern.compile(
                        "(-?\\d+|[A-Za-z_$][A-Za-z0-9_$]*)"
                                + "\\s*"
                                + "([+\\-*/])"
                                + "\\s*"
                                + "(-?\\d+|[A-Za-z_$][A-Za-z0-9_$]*)"
                ).matcher(expression);

        if (!arithmetic.matches()) {
            return null;
        }

        Integer left =
                getIntegerValue(
                        arithmetic.group(1)
                );

        Integer right =
                getIntegerValue(
                        arithmetic.group(3)
                );

        if (left == null || right == null) {
            return null;
        }

        String operator =
                arithmetic.group(2);

        switch (operator) {

            case "+":
                return left + right;

            case "-":
                return left - right;

            case "*":
                return left * right;

            case "/":

                if (right == 0) {
                    return null;
                }

                return left / right;

            default:
                return null;
        }
    }

    // =====================================================
    // GET INTEGER VALUE
    // =====================================================

    private static Integer getIntegerValue(
            String value) {

        value =
                value.trim();

        if (value.matches("-?\\d+")) {

            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return variables.get(value);
    }

    // =====================================================
    // ERROR REPORTER
    // =====================================================

    private static void reportLogicalError(
            int lineNumber,
            String errorType,
            String condition,
            String description) {

        String key =
                lineNumber
                        + "|"
                        + errorType
                        + "|"
                        + condition;

        if (reportedErrors.contains(key)) {
            return;
        }

        reportedErrors.add(key);

        System.out.println();
        System.out.println(
                "Logical Error Detected!"
        );

        System.out.println(
                "----------------------------------------"
        );

        System.out.println(
                "Error Type: "
                        + errorType
        );

        System.out.println(
                "Line: "
                        + lineNumber
        );

        System.out.println(
                "Condition / Expression: "
                        + condition
        );

        System.out.println(description);
    }

    // =====================================================
    // FINAL REPORT
    // =====================================================

    private static void printFinalReport(
            boolean syntaxError) {

        System.out.println();

        System.out.println(
                "========================================"
        );

        System.out.println(
                "          FINAL ANALYSIS REPORT"
        );

        System.out.println(
                "========================================"
        );

        if (syntaxError) {

            System.out.println(
                    "✗ Syntax errors detected."
            );

        } else {

            System.out.println(
                    "✓ No syntax errors found."
            );
        }

        if (!reportedErrors.isEmpty()) {

            System.out.println(
                    "✗ Logical errors detected."
            );

            System.out.println(
                    "Total unique logical errors detected: "
                            + reportedErrors.size()
            );

        } else {

            System.out.println(
                    "✓ No logical errors detected."
            );

            System.out.println(
                    "Total unique logical errors detected: 0"
            );
        }

        System.out.println(
                "========================================"
        );

        if (syntaxError
                && !reportedErrors.isEmpty()) {

            System.out.println();

            System.out.println(
                    "Both syntax and logical errors were detected."
            );

        } else if (syntaxError) {

            System.out.println();

            System.out.println(
                    "Only syntax errors were detected."
            );

        } else if (!reportedErrors.isEmpty()) {

            System.out.println();

            System.out.println(
                    "Only logical errors were detected."
            );

        } else {

            System.out.println();

            System.out.println(
                    "No syntax or logical errors were detected."
            );
        }
    }

    // =====================================================
    // REMOVE COMMENTS
    // =====================================================

    private static String removeComment(
            String line) {

        int commentIndex =
                line.indexOf("//");

        if (commentIndex >= 0) {

            return line.substring(
                    0,
                    commentIndex
            );
        }

        return line;
    }
}