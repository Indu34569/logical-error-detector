package com.project.analyzer;

import com.github.javaparser.JavaParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogicalErrorDetector {

    private static final Path INPUT_FILE =
            Path.of("test-input/tests/WebInput.java");

    private static final Map<String, Integer> variables =
            new LinkedHashMap<>();

    private static final Set<String> initializedVariables =
            new LinkedHashSet<>();

    private static final Set<String> assignedVariables =
            new LinkedHashSet<>();

    private static final Z3ConstraintAnalyzer z3Analyzer =
            new Z3ConstraintAnalyzer();

    private static final Set<String> reportedErrors =
            new LinkedHashSet<>();

    public static void main(String[] args) {

        System.out.println("========================================");
        System.out.println("       STATIC ANALYSIS TOOL");
        System.out.println("========================================");
        System.out.println();

        System.out.println("File: " + INPUT_FILE);
        System.out.println();

        if (!Files.exists(INPUT_FILE)) {
            System.out.println("ERROR: Input Java file not found.");
            return;
        }

        String source;

        try {
            source = Files.readString(INPUT_FILE);
        } catch (Exception e) {
            System.out.println("ERROR: Could not read Java file.");
            System.out.println(e.getMessage());
            return;
        }

        boolean syntaxError = performSyntaxAnalysis(source);

        variables.clear();
        initializedVariables.clear();
        assignedVariables.clear();
        reportedErrors.clear();

        System.out.println("LOGICAL ANALYSIS");
        System.out.println("----------------------------------------");

        analyzeVariables(source);
        analyzeConditions(source);
        analyzeDivisionByZero(source);
        analyzeConstantConditions(source);
        analyzeInfiniteLoops(source);
        analyzeUnreachableCode(source);
        analyzeUninitializedVariables(source);
        analyzeDeadAssignments(source);

        printFinalReport(syntaxError);
    }

    // =====================================================
    // SYNTAX ANALYSIS
    // =====================================================

    private static boolean performSyntaxAnalysis(String source) {

        System.out.println("SYNTAX ANALYSIS");
        System.out.println("----------------------------------------");

        boolean syntaxError = false;

        try {

            JavaParser parser = new JavaParser();

            var parseResult = parser.parse(source);

            if (!parseResult.getProblems().isEmpty()) {

                syntaxError = true;

                System.out.println("✗ Syntax errors detected!");
                System.out.println();
                System.out.println("Syntax Error Details:");
                System.out.println("----------------------------------------");

                parseResult.getProblems().forEach(
                        problem -> System.out.println(problem)
                );

                System.out.println();

            } else {

                System.out.println("✓ No syntax errors found.");
                System.out.println();
            }

        } catch (Exception e) {

            syntaxError = true;

            System.out.println("✗ Syntax errors detected!");
            System.out.println();
            System.out.println("Syntax Error Details:");
            System.out.println("----------------------------------------");
            System.out.println(e.getMessage());
            System.out.println();
        }

        return syntaxError;
    }

    // =====================================================
    // VARIABLE ANALYSIS
    // =====================================================

    private static void analyzeVariables(String source) {

        String[] lines = source.split("\\R", -1);

        System.out.println();
        System.out.println("PROGRAM-ORDER DATA-FLOW ANALYSIS");
        System.out.println("----------------------------------------");

        for (int i = 0; i < lines.length; i++) {

            String line = removeComment(lines[i]).trim();

            if (line.isEmpty()) {
                continue;
            }

            Pattern declarationPattern =
                    Pattern.compile(
                            "\\b(?:int|long|short|byte)\\s+"
                                    + "([A-Za-z_$][A-Za-z0-9_$]*)"
                                    + "\\s*=\\s*(.+?)\\s*;?$"
                    );

            Matcher declaration =
                    declarationPattern.matcher(line);

            if (declaration.find()) {

                String variable = declaration.group(1);
                String expression = declaration.group(2).trim();

                Integer value =
                        evaluateArithmeticExpression(expression);

                assignedVariables.add(variable);

                if (value != null) {

                    variables.put(variable, value);
                    initializedVariables.add(variable);

                    System.out.println(
                            "Line "
                                    + (i + 1)
                                    + ": Variable "
                                    + variable
                                    + " = "
                                    + value
                    );

                } else {

                    System.out.println(
                            "Line "
                                    + (i + 1)
                                    + ": Variable "
                                    + variable
                                    + " initialized with expression"
                    );
                }

                continue;
            }

            Pattern simpleDeclarationPattern =
                    Pattern.compile(
                            "\\b(?:int|long|short|byte)\\s+"
                                    + "([A-Za-z_$][A-Za-z0-9_$]*)\\s*;"
                    );

            Matcher simpleDeclaration =
                    simpleDeclarationPattern.matcher(line);

            if (simpleDeclaration.find()) {

                String variable = simpleDeclaration.group(1);

                assignedVariables.add(variable);

                continue;
            }

            Pattern assignmentPattern =
                    Pattern.compile(
                            "^([A-Za-z_$][A-Za-z0-9_$]*)"
                                    + "\\s*=\\s*(.+?)\\s*;?$"
                    );

            Matcher assignment =
                    assignmentPattern.matcher(line);

            if (assignment.find()) {

                String variable = assignment.group(1);
                String expression = assignment.group(2).trim();

                Integer value =
                        evaluateArithmeticExpression(expression);

                assignedVariables.add(variable);

                if (value != null) {

                    variables.put(variable, value);
                    initializedVariables.add(variable);

                    System.out.println(
                            "Line "
                                    + (i + 1)
                                    + ": Variable updated "
                                    + variable
                                    + " = "
                                    + value
                    );
                }
            }
        }

        if (variables.isEmpty()) {
            System.out.println("No integer variables found.");
        }

        System.out.println();
    }

    // =====================================================
    // ARITHMETIC EVALUATION
    // =====================================================

    private static Integer evaluateArithmeticExpression(
            String expression) {

        expression = expression.trim();

        while (expression.startsWith("(")
                && expression.endsWith(")")) {

            expression =
                    expression.substring(
                            1,
                            expression.length() - 1
                    ).trim();
        }

        if (expression.matches("-?\\d+")) {
            return Integer.parseInt(expression);
        }

        if (expression.matches(
                "[A-Za-z_$][A-Za-z0-9_$]*")) {

            return variables.get(expression);
        }

        int parentheses = 0;

        for (int i = expression.length() - 1;
             i >= 0;
             i--) {

            char c = expression.charAt(i);

            if (c == ')') {
                parentheses++;
            } else if (c == '(') {
                parentheses--;
            }

            if (parentheses == 0
                    && (c == '+' || c == '-')
                    && i > 0) {

                Integer left =
                        evaluateArithmeticExpression(
                                expression.substring(0, i)
                        );

                Integer right =
                        evaluateArithmeticExpression(
                                expression.substring(i + 1)
                        );

                if (left == null || right == null) {
                    return null;
                }

                if (c == '+') {
                    return left + right;
                }

                return left - right;
            }
        }

        parentheses = 0;

        for (int i = expression.length() - 1;
             i >= 0;
             i--) {

            char c = expression.charAt(i);

            if (c == ')') {
                parentheses++;
            } else if (c == '(') {
                parentheses--;
            }

            if (parentheses == 0
                    && (c == '*' || c == '/')) {

                Integer left =
                        evaluateArithmeticExpression(
                                expression.substring(0, i)
                        );

                Integer right =
                        evaluateArithmeticExpression(
                                expression.substring(i + 1)
                        );

                if (left == null || right == null) {
                    return null;
                }

                if (c == '*') {
                    return left * right;
                }

                if (right == 0) {
                    return null;
                }

                return left / right;
            }
        }

        return null;
    }

    // =====================================================
    // CONDITION ANALYSIS
    // =====================================================

    private static void analyzeConditions(String source) {

        String[] lines = source.split("\\R", -1);

        System.out.println("IF / ELSE / LOOP ANALYSIS");
        System.out.println("----------------------------------------");

        for (int i = 0; i < lines.length; i++) {

            String line = removeComment(lines[i]).trim();

            if (line.isEmpty()) {
                continue;
            }

            int lineNumber = i + 1;

            analyzeIfCondition(line, lineNumber);
            analyzeWhileCondition(line, lineNumber);
            analyzeForCondition(line, lineNumber);
            analyzeDoWhileCondition(line, lineNumber);
        }

        System.out.println();
    }

    private static void analyzeIfCondition(
            String line,
            int lineNumber) {

        Matcher matcher =
                Pattern.compile(
                        "\\bif\\s*\\(([^)]*)\\)"
                ).matcher(line);

        if (!matcher.find()) {
            return;
        }

        String condition = matcher.group(1).trim();

        System.out.println(
                "IF at line "
                        + lineNumber
                        + ": "
                        + condition
        );

        checkCondition(
                condition,
                lineNumber,
                "Impossible Relational Condition"
        );
    }

    private static void analyzeWhileCondition(
            String line,
            int lineNumber) {

        if (line.startsWith("do")) {
            return;
        }

        Matcher matcher =
                Pattern.compile(
                        "\\bwhile\\s*\\(([^)]*)\\)"
                ).matcher(line);

        if (!matcher.find()) {
            return;
        }

        String condition = matcher.group(1).trim();

        System.out.println(
                "WHILE at line "
                        + lineNumber
                        + ": "
                        + condition
        );

        checkCondition(
                condition,
                lineNumber,
                "Impossible Loop Condition"
        );
    }

    private static void analyzeForCondition(
            String line,
            int lineNumber) {

        Matcher matcher =
                Pattern.compile(
                        "\\bfor\\s*\\(([^;]*);\\s*([^;]*);\\s*([^)]*)\\)"
                ).matcher(line);

        if (!matcher.find()) {
            return;
        }

        String condition = matcher.group(2).trim();

        if (condition.isEmpty()) {
            return;
        }

        System.out.println(
                "FOR at line "
                        + lineNumber
                        + ": "
                        + condition
        );

        checkCondition(
                condition,
                lineNumber,
                "Impossible Loop Condition"
        );
    }

    private static void analyzeDoWhileCondition(
            String line,
            int lineNumber) {

        Matcher matcher =
                Pattern.compile(
                        "\\bdo\\s+while\\s*\\(([^)]*)\\)"
                ).matcher(line);

        if (!matcher.find()) {
            return;
        }

        String condition = matcher.group(1).trim();

        System.out.println(
                "DO-WHILE at line "
                        + lineNumber
                        + ": "
                        + condition
        );

        checkCondition(
                condition,
                lineNumber,
                "Impossible Loop Condition"
        );
    }

    // =====================================================
    // Z3 CONDITION CHECK
    // =====================================================

    private static void checkCondition(
            String condition,
            int lineNumber,
            String errorType) {

        Matcher matcher =
                Pattern.compile(
                        "^([A-Za-z_$][A-Za-z0-9_$]*)"
                                + "\\s*(<=|>=|==|!=|<|>)"
                                + "\\s*(-?\\d+)$"
                ).matcher(condition);

        if (!matcher.find()) {
            return;
        }

        String variable = matcher.group(1);

        String operator = matcher.group(2);

        int rightValue =
                Integer.parseInt(matcher.group(3));

        if (!variables.containsKey(variable)) {
            return;
        }

        int leftValue = variables.get(variable);

        boolean result =
                z3Analyzer.isSatisfiable(
                        leftValue,
                        operator,
                        rightValue
                );

        System.out.println(
                "Known variable values: "
                        + variables
        );

        System.out.println(
                "Generated Constraint: "
                        + condition
        );

        if (!result) {

            String description;

            if (errorType.equals(
                    "Impossible Loop Condition")) {

                description =
                        "The loop condition is false for the known "
                                + "variable values.";

            } else {

                description =
                        "Condition '"
                                + condition
                                + "' is false for the known "
                                + "variable values.";
            }

            reportLogicalError(
                    lineNumber,
                    errorType,
                    condition,
                    description
            );
        }
    }

    // =====================================================
    // DIVISION BY ZERO
    // =====================================================

    private static void analyzeDivisionByZero(
            String source) {

        System.out.println(
                "DIVISION-BY-ZERO ANALYSIS"
        );

        System.out.println(
                "----------------------------------------"
        );

        String[] lines = source.split("\\R", -1);

        boolean found = false;

        Pattern divisionPattern =
                Pattern.compile(
                        "([A-Za-z_$][A-Za-z0-9_$]*|-?\\d+)"
                                + "\\s*/\\s*"
                                + "([A-Za-z_$][A-Za-z0-9_$]*|-?\\d+)"
                );

        for (int i = 0; i < lines.length; i++) {

            String line =
                    removeComment(lines[i]).trim();

            Matcher matcher =
                    divisionPattern.matcher(line);

            while (matcher.find()) {

                String divisor = matcher.group(2);

                Integer divisorValue = null;

                if (divisor.matches("-?\\d+")) {

                    divisorValue =
                            Integer.parseInt(divisor);

                } else if (variables.containsKey(divisor)) {

                    divisorValue =
                            variables.get(divisor);
                }

                if (divisorValue != null
                        && divisorValue == 0) {

                    found = true;

                    String expression =
                            matcher.group();

                    reportLogicalError(
                            i + 1,
                            "Division By Zero",
                            expression,
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

        String[] lines = source.split("\\R", -1);

        boolean found = false;

        for (int i = 0; i < lines.length; i++) {

            String line =
                    removeComment(lines[i]).trim();

            int lineNumber = i + 1;

            Matcher ifMatcher =
                    Pattern.compile(
                            "\\bif\\s*\\(([^)]*)\\)"
                    ).matcher(line);

            if (ifMatcher.find()) {

                String condition =
                        ifMatcher.group(1).trim();

                Boolean result =
                        evaluateConstantCondition(
                                condition
                        );

                if (result != null) {

                    found = true;

                    reportLogicalError(
                            lineNumber,
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
                        "(-?\\d+)\\s*(<=|>=|==|!=|<|>)\\s*(-?\\d+)"
                ).matcher(condition);

        if (!matcher.matches()) {
            return null;
        }

        int left =
                Integer.parseInt(matcher.group(1));

        int right =
                Integer.parseInt(matcher.group(3));

        String operator = matcher.group(2);

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

        String[] lines = source.split("\\R", -1);

        boolean found = false;

        for (int i = 0; i < lines.length; i++) {

            String line =
                    removeComment(lines[i]).trim();

            int lineNumber = i + 1;

            if (line.matches(
                    "while\\s*\\(\\s*true\\s*\\).*")) {

                found = true;

                reportLogicalError(
                        lineNumber,
                        "Potential Infinite Loop",
                        "while(true)",
                        "The while loop condition is always true."
                );
            }

            if (line.matches(
                    "for\\s*\\(\\s*;\\s*;\\s*\\).*")) {

                found = true;

                reportLogicalError(
                        lineNumber,
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

        String[] lines = source.split("\\R", -1);

        boolean found = false;

        boolean afterReturn = false;
        boolean afterBreak = false;
        boolean afterContinue = false;

        for (int i = 0; i < lines.length; i++) {

            String line =
                    removeComment(lines[i]).trim();

            if (line.isEmpty()) {
                continue;
            }

            int lineNumber = i + 1;

            if (afterReturn
                    || afterBreak
                    || afterContinue) {

                if (!line.equals("}")
                        && !line.startsWith("else")
                        && !line.startsWith("catch")
                        && !line.startsWith("finally")) {

                    found = true;

                    reportLogicalError(
                            lineNumber,
                            "Unreachable Code",
                            line,
                            "This statement cannot be reached because "
                                    + "the previous control-flow statement "
                                    + "terminates or skips execution."
                    );

                    afterReturn = false;
                    afterBreak = false;
                    afterContinue = false;
                }
            }

            if (line.matches(
                    "return\\s*.*;")) {

                afterReturn = true;
            }

            if (line.matches(
                    "break\\s*;")) {

                afterBreak = true;
            }

            if (line.matches(
                    "continue\\s*;")) {

                afterContinue = true;
            }

            if (line.equals("}")) {

                afterReturn = false;
                afterBreak = false;
                afterContinue = false;
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

        String[] lines = source.split("\\R", -1);

        boolean found = false;

        Set<String> declared =
                new LinkedHashSet<>();

        Set<String> initialized =
                new LinkedHashSet<>();

        Pattern declarationWithValue =
                Pattern.compile(
                        "\\b(?:int|long|short|byte)\\s+"
                                + "([A-Za-z_$][A-Za-z0-9_$]*)"
                                + "\\s*=\\s*(.+?);?$"
                );

        Pattern declarationOnly =
                Pattern.compile(
                        "\\b(?:int|long|short|byte)\\s+"
                                + "([A-Za-z_$][A-Za-z0-9_$]*)\\s*;"
                );

        Pattern variableUse =
                Pattern.compile(
                        "\\b([A-Za-z_$][A-Za-z0-9_$]*)\\b"
                );

        for (int i = 0; i < lines.length; i++) {

            String line =
                    removeComment(lines[i]).trim();

            if (line.isEmpty()) {
                continue;
            }

            int lineNumber = i + 1;

            Matcher declaration =
                    declarationWithValue.matcher(line);

            if (declaration.find()) {

                String variable =
                        declaration.group(1);

                declared.add(variable);
                initialized.add(variable);

                continue;
            }

            Matcher emptyDeclaration =
                    declarationOnly.matcher(line);

            if (emptyDeclaration.find()) {

                String variable =
                        emptyDeclaration.group(1);

                declared.add(variable);

                continue;
            }

            Matcher useMatcher =
                    variableUse.matcher(line);

            while (useMatcher.find()) {

                String variable =
                        useMatcher.group(1);

                if (!declared.contains(variable)) {
                    continue;
                }

                if (initialized.contains(variable)) {
                    continue;
                }

                if (line.startsWith(variable + " =")) {
                    initialized.add(variable);
                    continue;
                }

                if (line.startsWith(
                        "int " + variable)) {
                    continue;
                }

                found = true;

                reportLogicalError(
                        lineNumber,
                        "Variable Used Before Initialization",
                        variable,
                        "Variable '" + variable
                                + "' is used before it is initialized."
                );

                initialized.add(variable);
                break;
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

        String[] lines = source.split("\\R", -1);

        boolean found = false;

        Map<String, Integer> lastAssignment =
                new LinkedHashMap<>();

        for (int i = 0; i < lines.length; i++) {

            String line =
                    removeComment(lines[i]).trim();

            if (line.isEmpty()) {
                continue;
            }

            int lineNumber = i + 1;

            Matcher assignment =
                    Pattern.compile(
                            "^(?:int|long|short|byte)?\\s*"
                                    + "([A-Za-z_$][A-Za-z0-9_$]*)"
                                    + "\\s*=\\s*(.+);$"
                    ).matcher(line);

            if (!assignment.find()) {
                continue;
            }

            String variable =
                    assignment.group(1);

            String expression =
                    assignment.group(2);

            if (lastAssignment.containsKey(variable)) {

                int previousLine =
                        lastAssignment.get(variable);

                found = true;

                reportLogicalError(
                        previousLine,
                        "Dead Assignment",
                        variable,
                        "The value assigned to '" + variable
                                + "' is overwritten before it is used."
                );
            }

            lastAssignment.put(
                    variable,
                    lineNumber
            );

            /*
             * Remove the assignment from consideration if
             * the variable is used in the same expression.
             */
            if (expression.contains(variable)) {
                lastAssignment.remove(variable);
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