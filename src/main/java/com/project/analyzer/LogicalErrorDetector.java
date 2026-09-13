package com.project.analyzer;

import com.github.javaparser.JavaParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogicalErrorDetector {

    private static final Path INPUT_FILE =
            Path.of("test-input/tests/WebInput.java");

    private static final Map<String, Integer> variables =
            new LinkedHashMap<>();

    /*
     * Stores logical errors already reported.
     * This prevents the same error from being reported twice.
     */
    private static final java.util.Set<String> reportedErrors =
            new java.util.LinkedHashSet<>();

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

        /*
         * =====================================================
         * STEP 1 - SYNTAX ANALYSIS
         * =====================================================
         */

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

                parseResult.getProblems().forEach(problem -> {
                    System.out.println(problem);
                });

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

        /*
         * =====================================================
         * STEP 2 - LOGICAL ANALYSIS
         *
         * IMPORTANT:
         * Logical analysis is performed EVEN when syntax errors
         * are present.
         * =====================================================
         */

        System.out.println("LOGICAL ANALYSIS");
        System.out.println("----------------------------------------");

        variables.clear();
        reportedErrors.clear();

        analyzeVariables(source);

        analyzeConditions(source);

        /*
         * =====================================================
         * FINAL RESULT
         * =====================================================
         */

        System.out.println();

        System.out.println("========================================");
        System.out.println("          FINAL ANALYSIS REPORT");
        System.out.println("========================================");

        if (syntaxError) {

            System.out.println("✗ Syntax errors detected.");

        } else {

            System.out.println("✓ No syntax errors found.");
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

        System.out.println("========================================");

        /*
         * Important message for the server/frontend.
         */
        if (syntaxError && !reportedErrors.isEmpty()) {

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

    /*
     * =====================================================
     * VARIABLE ANALYSIS
     * =====================================================
     */

    private static void analyzeVariables(String source) {

        String[] lines =
                source.split("\\R", -1);

        System.out.println();
        System.out.println(
                "PROGRAM-ORDER DATA-FLOW ANALYSIS"
        );
        System.out.println("----------------------------------------");

        for (int i = 0; i < lines.length; i++) {

            String originalLine =
                    lines[i];

            String line =
                    removeComment(originalLine).trim();

            if (line.isEmpty()) {
                continue;
            }

            /*
             * int age = 20;
             * int marks = 40;
             */
            Pattern declarationPattern =
                    Pattern.compile(
                            "\\b(?:int|long|short|byte)\\s+"
                                    + "([A-Za-z_$][A-Za-z0-9_$]*)"
                                    + "\\s*=\\s*(-?\\d+)\\s*;?"
                    );

            Matcher declaration =
                    declarationPattern.matcher(line);

            if (declaration.find()) {

                String variable =
                        declaration.group(1);

                int value =
                        Integer.parseInt(
                                declaration.group(2)
                        );

                variables.put(
                        variable,
                        value
                );

                System.out.println(
                        "Line "
                                + (i + 1)
                                + ": Variable "
                                + variable
                                + " = "
                                + value
                );

                continue;
            }

            /*
             * age = 20;
             */
            Pattern assignmentPattern =
                    Pattern.compile(
                            "^([A-Za-z_$][A-Za-z0-9_$]*)"
                                    + "\\s*=\\s*(-?\\d+)\\s*;?"
                    );

            Matcher assignment =
                    assignmentPattern.matcher(line);

            if (assignment.find()) {

                String variable =
                        assignment.group(1);

                int value =
                        Integer.parseInt(
                                assignment.group(2)
                        );

                variables.put(
                        variable,
                        value
                );

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

        if (variables.isEmpty()) {

            System.out.println(
                    "No simple integer variables found."
            );
        }

        System.out.println();
    }

    /*
     * =====================================================
     * CONDITION ANALYSIS
     * =====================================================
     */

    private static void analyzeConditions(
            String source) {

        String[] lines =
                source.split("\\R", -1);

        System.out.println(
                "IF / ELSE / LOOP ANALYSIS"
        );
        System.out.println(
                "----------------------------------------"
        );

        for (int i = 0; i < lines.length; i++) {

            String originalLine =
                    lines[i];

            String line =
                    removeComment(originalLine).trim();

            if (line.isEmpty()) {
                continue;
            }

            int lineNumber =
                    i + 1;

            analyzeIfCondition(
                    line,
                    lineNumber
            );

            analyzeWhileCondition(
                    line,
                    lineNumber
            );

            analyzeForCondition(
                    line,
                    lineNumber
            );

            analyzeDoWhileCondition(
                    line,
                    lineNumber
            );
        }

        System.out.println();
    }

    /*
     * =====================================================
     * IF
     * =====================================================
     */

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

        String condition =
                matcher.group(1).trim();

        System.out.println(
                "IF at line "
                        + lineNumber
                        + ": "
                        + condition
        );

        checkCondition(
                condition,
                lineNumber,
                "IF"
        );
    }

    /*
     * =====================================================
     * WHILE
     * =====================================================
     */

    private static void analyzeWhileCondition(
            String line,
            int lineNumber) {

        /*
         * Avoid matching do-while here.
         */
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

        String condition =
                matcher.group(1).trim();

        System.out.println(
                "WHILE at line "
                        + lineNumber
                        + ": "
                        + condition
        );

        checkCondition(
                condition,
                lineNumber,
                "WHILE"
        );
    }

    /*
     * =====================================================
     * FOR
     * =====================================================
     */

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

        String condition =
                matcher.group(2).trim();

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
                "FOR"
        );
    }

    /*
     * =====================================================
     * DO-WHILE
     * =====================================================
     */

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

        String condition =
                matcher.group(1).trim();

        System.out.println(
                "DO-WHILE at line "
                        + lineNumber
                        + ": "
                        + condition
        );

        checkCondition(
                condition,
                lineNumber,
                "DO-WHILE"
        );
    }

    /*
     * =====================================================
     * CHECK CONDITION
     * =====================================================
     */

    private static void checkCondition(
            String condition,
            int lineNumber,
            String statementType) {

        /*
         * Supported:
         *
         * age < 10
         * age > 10
         * age <= 10
         * age >= 10
         * age == 10
         * age != 10
         */
        Matcher matcher =
                Pattern.compile(
                        "^([A-Za-z_$][A-Za-z0-9_$]*)"
                                + "\\s*(<=|>=|==|!=|<|>)"
                                + "\\s*(-?\\d+)$"
                ).matcher(condition);

        if (!matcher.find()) {
            return;
        }

        String variable =
                matcher.group(1);

        String operator =
                matcher.group(2);

        int rightValue =
                Integer.parseInt(
                        matcher.group(3)
                );

        if (!variables.containsKey(variable)) {
            return;
        }

        int leftValue =
                variables.get(variable);

        boolean result =
                evaluate(
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

        /*
         * If the condition is false for a known constant
         * value, it is an impossible relational condition.
         */
        if (!result) {

            reportLogicalError(
                    lineNumber,
                    "Impossible Relational Condition",
                    condition,
                    "Condition '"
                            + condition
                            + "' is false for the known "
                            + "variable values."
            );
        }
    }

    /*
     * =====================================================
     * LOGICAL ERROR REPORTER
     * =====================================================
     */

    private static void reportLogicalError(
            int lineNumber,
            String errorType,
            String condition,
            String description) {

        /*
         * Unique key:
         *
         * line + error type + condition
         *
         * Therefore the same logical error can never
         * appear twice.
         */
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

        /*
         * IMPORTANT FOR FRONTEND:
         * The JavaScript parser can use this line.
         */
        System.out.println(
                "Line: "
                        + lineNumber
        );

        System.out.println(
                "Condition: "
                        + condition
        );

        System.out.println(
                description
        );

        System.out.println();
    }

    /*
     * =====================================================
     * EXPRESSION EVALUATION
     * =====================================================
     */

    private static boolean evaluate(
            int left,
            String operator,
            int right) {

        switch (operator) {

            case "<":
                return left < right;

            case ">":
                return left > right;

            case "<=":
                return left <= right;

            case ">=":
                return left >= right;

            case "==":
                return left == right;

            case "!=":
                return left != right;

            default:
                return true;
        }
    }

    /*
     * =====================================================
     * REMOVE COMMENTS
     * =====================================================
     */

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