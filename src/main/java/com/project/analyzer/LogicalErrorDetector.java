package com.project.analyzer;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.WhileStmt;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LogicalErrorDetector {

    /*
     * Store unique logical errors.
     *
     * The same condition can be discovered by more than one
     * analysis phase. We therefore store a unique key instead
     * of increasing the counter every time.
     */
    private static final Set<String> detectedErrors =
            new HashSet<>();

    public static void main(String[] args) {

        Path filePath =
                Path.of("test-input/tests/TestUnreachableWhile.java");

        System.out.println("========================================");
        System.out.println("       STATIC ANALYSIS TOOL");
        System.out.println("========================================");
        System.out.println();

        System.out.println("File: " + filePath);
        System.out.println();

        try {

            // ==================================================
            // STEP 1: READ SOURCE FILE
            // ==================================================

            String code =
                    Files.readString(filePath);

            // ==================================================
            // STEP 2: SYNTAX ANALYSIS
            // ==================================================

            System.out.println("SYNTAX ANALYSIS");
            System.out.println("----------------------------------------");

            CompilationUnit cu;

            try {

                cu = StaticJavaParser.parse(code);

                System.out.println(
                        "✓ No syntax errors found"
                );

            } catch (Exception e) {

                System.out.println(
                        "✗ Syntax errors detected!"
                );

                System.out.println();

                System.out.println(
                        "Syntax Error Details:"
                );

                System.out.println(
                        "----------------------------------------"
                );

                System.out.println(
                        e.getMessage()
                );

                System.out.println();

                System.out.println(
                        "========================================"
                );

                System.out.println(
                        "Analysis stopped because the Java"
                );

                System.out.println(
                        "program contains syntax errors."
                );

                System.out.println(
                        "========================================"
                );

                return;
            }

            System.out.println();

            // ==================================================
            // STEP 3: PROGRAM-ORDER DATA-FLOW ANALYSIS
            // ==================================================

            System.out.println(
                    "PROGRAM-ORDER DATA-FLOW ANALYSIS"
            );

            System.out.println(
                    "----------------------------------------"
            );

            Map<String, Integer> variables =
                    new HashMap<>();

            for (VariableDeclarator variable
                    : cu.findAll(VariableDeclarator.class)) {

                if (variable.getInitializer().isPresent()) {

                    Expression initializer =
                            variable.getInitializer().get();

                    String variableName =
                            variable.getNameAsString();

                    Integer value =
                            resolveIntegerValue(
                                    initializer,
                                    variables
                            );

                    if (value != null) {

                        variables.put(
                                variableName,
                                value
                        );

                        System.out.println(
                                "Line "
                                        + getLine(variable)
                                        + ": Variable "
                                        + variableName
                                        + " = "
                                        + value
                        );
                    }
                }
            }

            // ==================================================
            // TRACK SIMPLE INTEGER ASSIGNMENTS
            // ==================================================

            for (AssignExpr assignment
                    : cu.findAll(AssignExpr.class)) {

                Expression target =
                        assignment.getTarget();

                Expression value =
                        assignment.getValue();

                if (target instanceof NameExpr) {

                    Integer integerValue =
                            resolveIntegerValue(
                                    value,
                                    variables
                            );

                    if (integerValue != null) {

                        String variableName =
                                target.toString();

                        variables.put(
                                variableName,
                                integerValue
                        );

                        System.out.println(
                                "Line "
                                        + getLine(assignment)
                                        + ": Assignment "
                                        + variableName
                                        + " = "
                                        + integerValue
                        );
                    }
                }
            }

            System.out.println();

            // ==================================================
            // CREATE ANALYZERS
            // ==================================================

            ConditionAnalyzer analyzer =
                    new ConditionAnalyzer();

            Z3ConstraintAnalyzer z3Analyzer =
                    new Z3ConstraintAnalyzer();

            ConstraintGenerator constraintGenerator =
                    new ConstraintGenerator();

            // Prevent unused-variable warning in some IDEs.
            if (analyzer == null) {
                System.out.println();
            }

            // ==================================================
            // STEP 4: IF / ELSE ANALYSIS
            // ==================================================

            System.out.println("IF / ELSE ANALYSIS");
            System.out.println("----------------------------------------");

            for (IfStmt ifStmt
                    : cu.findAll(IfStmt.class)) {

                int line =
                        getLine(ifStmt);

                String condition =
                        ifStmt.getCondition().toString();

                System.out.println(
                        "Condition at line "
                                + line
                                + ": "
                                + condition
                );

                if (ifStmt.getCondition()
                        instanceof BinaryExpr) {

                    BinaryExpr binaryExpr =
                            (BinaryExpr)
                                    ifStmt.getCondition();

                    System.out.println(
                            "  Left side: "
                                    + binaryExpr.getLeft()
                    );

                    System.out.println(
                            "  Operator: "
                                    + binaryExpr.getOperator()
                    );

                    System.out.println(
                            "  Right side: "
                                    + binaryExpr.getRight()
                    );
                }

                System.out.println(
                        "  Known variables: "
                                + variables
                );

                System.out.println();
            }

            // ==================================================
            // STEP 5: LOGICAL ERROR ANALYSIS
            // ==================================================

            System.out.println("LOGICAL ERROR ANALYSIS");
            System.out.println("----------------------------------------");

            for (IfStmt ifStmt
                    : cu.findAll(IfStmt.class)) {

                int line =
                        getLine(ifStmt);

                String condition =
                        ifStmt.getCondition().toString();

                System.out.println(
                        "Condition at line "
                                + line
                                + ": "
                                + condition
                );

                System.out.println(
                        "Known variable values: "
                                + variables
                );

                String generatedConstraint =
                        constraintGenerator.generateConstraint(
                                ifStmt.getCondition()
                        );

                System.out.println(
                        "Generated Constraint: "
                                + generatedConstraint
                );

                boolean z3Result = true;

                if (ifStmt.getCondition()
                        instanceof BinaryExpr) {

                    BinaryExpr binaryExpr =
                            (BinaryExpr)
                                    ifStmt.getCondition();

                    z3Result =
                            z3Analyzer.isSatisfiable(
                                    binaryExpr,
                                    variables
                            );

                    if (z3Result) {

                        System.out.println(
                                "Z3 Result: SATISFIABLE"
                        );

                    } else {

                        System.out.println(
                                "Z3 Result: UNSATISFIABLE"
                        );
                    }
                }

                if (!z3Result) {

                    reportLogicalError(
                            line,
                            condition,
                            "UNSATISFIABLE_CONDITION"
                    );

                    System.out.println();

                    System.out.println(
                            "Logical Error Detected!"
                    );

                    classifyLogicalError(
                            ifStmt,
                            variables,
                            condition
                    );
                }

                System.out.println();
            }

            // ==================================================
            // STEP 6: NESTED IF ANALYSIS
            // ==================================================

            System.out.println("NESTED IF ANALYSIS");
            System.out.println("----------------------------------------");

            boolean nestedFound = false;

            for (IfStmt outerIf
                    : cu.findAll(IfStmt.class)) {

                for (IfStmt innerIf
                        : outerIf.findAll(IfStmt.class)) {

                    if (innerIf == outerIf) {
                        continue;
                    }

                    nestedFound = true;

                    int outerLine =
                            getLine(outerIf);

                    int innerLine =
                            getLine(innerIf);

                    String innerCondition =
                            innerIf.getCondition()
                                    .toString();

                    System.out.println(
                            "Outer IF at line "
                                    + outerLine
                                    + ": "
                                    + outerIf.getCondition()
                    );

                    System.out.println(
                            "  Nested IF at line "
                                    + innerLine
                                    + ": "
                                    + innerCondition
                    );

                    System.out.println(
                            "  Known variables: "
                                    + variables
                    );

                    if (innerIf.getCondition()
                            instanceof BinaryExpr) {

                        BinaryExpr binaryExpr =
                                (BinaryExpr)
                                        innerIf.getCondition();

                        boolean reachable =
                                z3Analyzer.isSatisfiable(
                                        binaryExpr,
                                        variables
                                );

                        if (!reachable) {

                            reportLogicalError(
                                    innerLine,
                                    innerCondition,
                                    "UNREACHABLE_NESTED_IF"
                            );

                            System.out.println();

                            System.out.println(
                                    "  Logical Error Detected!"
                            );

                            System.out.println(
                                    "  Error Type: "
                                            + "Unreachable Nested TRUE Branch"
                            );

                            System.out.println(
                                    "  Nested condition '"
                                            + innerCondition
                                            + "' can never be true."
                            );

                        } else {

                            System.out.println(
                                    "  Nested condition: reachable"
                            );
                        }
                    }

                    System.out.println();
                }
            }

            if (!nestedFound) {

                System.out.println(
                        "No nested IF statements found."
                );
            }

            System.out.println();

            // ==================================================
            // STEP 7: BRANCH ANALYSIS
            // ==================================================

            System.out.println("BRANCH ANALYSIS");
            System.out.println("----------------------------------------");

            for (IfStmt ifStmt
                    : cu.findAll(IfStmt.class)) {

                int line =
                        getLine(ifStmt);

                String condition =
                        ifStmt.getCondition().toString();

                System.out.println(
                        "IF at line "
                                + line
                                + ": "
                                + condition
                );

                if (!(ifStmt.getCondition()
                        instanceof BinaryExpr)) {

                    System.out.println(
                            "  Complex condition - "
                                    + "branch analysis skipped."
                    );

                    continue;
                }

                BinaryExpr binaryExpr =
                        (BinaryExpr)
                                ifStmt.getCondition();

                boolean conditionSatisfiable =
                        z3Analyzer.isSatisfiable(
                                binaryExpr,
                                variables
                        );

                // ==================================================
                // TRUE BRANCH
                // ==================================================

                if (!conditionSatisfiable) {

                    reportLogicalError(
                            line,
                            condition,
                            "UNREACHABLE_TRUE_BRANCH"
                    );

                    System.out.println();

                    System.out.println(
                            "Logical Error Detected!"
                    );

                    System.out.println(
                            "Error Type: "
                                    + "Unreachable TRUE Branch"
                    );

                    System.out.println(
                            "Condition '"
                                    + condition
                                    + "' can never be true "
                                    + "for the known variable values."
                    );

                    System.out.println(
                            "TRUE branch is unreachable."
                    );

                    if (ifStmt.getElseStmt().isPresent()) {

                        System.out.println(
                                "FALSE branch is reachable."
                        );
                    }

                    continue;
                }

                // ==================================================
                // ELSE BRANCH
                // ==================================================

                if (ifStmt.getElseStmt().isPresent()) {

                    boolean conditionAlwaysTrue =
                            isConditionAlwaysTrue(
                                    binaryExpr,
                                    variables
                            );

                    if (conditionAlwaysTrue) {

                        reportLogicalError(
                                line,
                                condition,
                                "UNREACHABLE_ELSE_BRANCH"
                        );

                        System.out.println();

                        System.out.println(
                                "Logical Error Detected!"
                        );

                        System.out.println(
                                "Error Type: "
                                        + "Unreachable ELSE Branch"
                        );

                        System.out.println(
                                "Condition '"
                                        + condition
                                        + "' is always true "
                                        + "for the known variable values."
                        );

                        System.out.println(
                                "ELSE branch can never execute."
                        );

                    } else {

                        System.out.println(
                                "  TRUE branch: reachable"
                        );

                        System.out.println(
                                "  ELSE branch: reachable"
                        );
                    }

                } else {

                    System.out.println(
                            "  TRUE branch: reachable"
                    );

                    System.out.println(
                            "  No ELSE branch present."
                    );
                }
            }

            System.out.println();

            // ==================================================
            // STEP 8: WHILE LOOP ANALYSIS
            // ==================================================

            System.out.println("WHILE LOOP ANALYSIS");
            System.out.println("----------------------------------------");

            for (WhileStmt whileStmt
                    : cu.findAll(WhileStmt.class)) {

                analyzeWhileLoop(
                        whileStmt,
                        variables,
                        z3Analyzer
                );
            }

            System.out.println();

            // ==================================================
            // STEP 9: DO-WHILE LOOP ANALYSIS
            // ==================================================

            System.out.println("DO-WHILE LOOP ANALYSIS");
            System.out.println("----------------------------------------");

            for (DoStmt doStmt
                    : cu.findAll(DoStmt.class)) {

                analyzeDoWhileLoop(
                        doStmt,
                        variables,
                        z3Analyzer
                );
            }

            System.out.println();

            // ==================================================
            // STEP 10: FOR LOOP ANALYSIS
            // ==================================================

            System.out.println("FOR LOOP ANALYSIS");
            System.out.println("----------------------------------------");

            for (ForStmt forStmt
                    : cu.findAll(ForStmt.class)) {

                analyzeForLoop(
                        forStmt,
                        variables,
                        z3Analyzer
                );
            }

            System.out.println();

            // ==================================================
            // STEP 11: LOOP CONTROL ANALYSIS
            // ==================================================

            System.out.println("LOOP CONTROL ANALYSIS");
            System.out.println("----------------------------------------");

            int breakCount =
                    cu.findAll(BreakStmt.class).size();

            int continueCount =
                    cu.findAll(ContinueStmt.class).size();

            if (breakCount == 0
                    && continueCount == 0) {

                System.out.println(
                        "No break/continue statements found."
                );

            } else {

                System.out.println(
                        "BREAK statements found: "
                                + breakCount
                );

                System.out.println(
                        "CONTINUE statements found: "
                                + continueCount
                );

                for (BreakStmt breakStmt
                        : cu.findAll(BreakStmt.class)) {

                    System.out.println(
                            "  break at line "
                                    + getLine(breakStmt)
                    );
                }

                for (ContinueStmt continueStmt
                        : cu.findAll(ContinueStmt.class)) {

                    System.out.println(
                            "  continue at line "
                                    + getLine(continueStmt)
                    );
                }
            }

            System.out.println();

            // ==================================================
            // STEP 12: INFINITE LOOP ANALYSIS
            // ==================================================

            System.out.println("INFINITE LOOP ANALYSIS");
            System.out.println("----------------------------------------");

            analyzeInfiniteLoops(cu);

            System.out.println();

            // ==================================================
            // STEP 13: COMPOUND CONDITION ANALYSIS
            // ==================================================

            System.out.println("COMPOUND CONDITION ANALYSIS");
            System.out.println("----------------------------------------");

            analyzeCompoundConditions(
                    cu,
                    variables,
                    z3Analyzer
            );

            // ==================================================
            // STEP 14: FINAL REPORT
            // ==================================================

            System.out.println();

            System.out.println("========================================");
            System.out.println("          FINAL ANALYSIS REPORT");
            System.out.println("========================================");

            if (detectedErrors.size() > 0) {

                System.out.println(
                        "✗ Logical errors detected."
                );

            } else {

                System.out.println(
                        "✓ No logical errors detected."
                );
            }

            System.out.println();

            System.out.println(
                    "Total unique logical errors detected: "
                            + detectedErrors.size()
            );

            System.out.println(
                    "========================================"
            );

            z3Analyzer.close();

        } catch (Exception e) {

            System.out.println();

            System.out.println(
                    "ERROR WHILE ANALYZING JAVA PROGRAM"
            );

            System.out.println(
                    "----------------------------------------"
            );

            System.out.println(
                    e.getMessage()
            );
        }
    }

    // ==========================================================
    // REGISTER UNIQUE ERROR
    // ==========================================================

    private static void reportLogicalError(
            int line,
            String condition,
            String errorType) {

        String key =
                line
                        + "|"
                        + errorType
                        + "|"
                        + condition;

        detectedErrors.add(key);
    }

    // ==========================================================
    // WHILE LOOP ANALYSIS
    // ==========================================================

    private static void analyzeWhileLoop(
            WhileStmt whileStmt,
            Map<String, Integer> variables,
            Z3ConstraintAnalyzer z3Analyzer) {

        int line =
                getLine(whileStmt);

        String condition =
                whileStmt.getCondition().toString();

        System.out.println(
                "WHILE at line "
                        + line
                        + ": "
                        + condition
        );

        System.out.println(
                "  Known variables: "
                        + variables
        );

        // --------------------------------------------------
        // while(true)
        // --------------------------------------------------

        if (isLiteralTrue(
                whileStmt.getCondition())) {

            if (!containsBreak(whileStmt)) {

                reportLogicalError(
                        line,
                        condition,
                        "INFINITE_WHILE"
                );

                System.out.println(
                        "  Logical Error Detected!"
                );

                System.out.println(
                        "  Error Type: Infinite Loop"
                );

                System.out.println(
                        "  WHILE condition is always true."
                );

                System.out.println(
                        "  No break statement found."
                );

            } else {

                System.out.println(
                        "  Constant TRUE loop detected."
                );

                System.out.println(
                        "  break statement provides an exit."
                );
            }

            return;
        }

        // --------------------------------------------------
        // Binary condition
        // --------------------------------------------------

        if (whileStmt.getCondition()
                instanceof BinaryExpr) {

            BinaryExpr conditionExpr =
                    (BinaryExpr)
                            whileStmt.getCondition();

            boolean satisfiable =
                    z3Analyzer.isSatisfiable(
                            conditionExpr,
                            variables
                    );

            if (!satisfiable) {

                reportLogicalError(
                        line,
                        condition,
                        "UNREACHABLE_WHILE"
                );

                System.out.println(
                        "Logical Error Detected!"
                );

                System.out.println(
                        "Error Type: Unreachable Loop Body"
                );

                System.out.println(
                        "Loop condition '"
                                + condition
                                + "' is false for "
                                + "the known variable values."
                );

                System.out.println(
                        "  WHILE body can never execute."
                );

            } else {

                System.out.println(
                        "  Loop condition: reachable"
                );
            }
        }
    }

    // ==========================================================
    // DO-WHILE LOOP ANALYSIS
    // ==========================================================

    private static void analyzeDoWhileLoop(
            DoStmt doStmt,
            Map<String, Integer> variables,
            Z3ConstraintAnalyzer z3Analyzer) {

        int line =
                getLine(doStmt);

        String condition =
                doStmt.getCondition().toString();

        System.out.println(
                "DO-WHILE at line "
                        + line
                        + ": "
                        + condition
        );

        System.out.println(
                "  Known variables: "
                        + variables
        );

        System.out.println(
                "  First execution: reachable"
        );

        // --------------------------------------------------
        // false
        // --------------------------------------------------

        if (isLiteralFalse(
                doStmt.getCondition())) {

            System.out.println(
                    "  Condition is always false."
            );

            System.out.println(
                    "  Loop executes exactly once."
            );

            return;
        }

        // --------------------------------------------------
        // true
        // --------------------------------------------------

        if (isLiteralTrue(
                doStmt.getCondition())) {

            if (!containsBreak(doStmt)) {

                reportLogicalError(
                        line,
                        condition,
                        "INFINITE_DO_WHILE"
                );

                System.out.println(
                        "  Logical Error Detected!"
                );

                System.out.println(
                        "  Error Type: Infinite DO-WHILE Loop"
                );

                System.out.println(
                        "  Condition is always true."
                );

                System.out.println(
                        "  No break statement found."
                );

            } else {

                System.out.println(
                        "  Condition is always true,"
                );

                System.out.println(
                        "  but break provides an exit."
                );
            }

            return;
        }

        // --------------------------------------------------
        // Binary condition
        // --------------------------------------------------

        if (doStmt.getCondition()
                instanceof BinaryExpr) {

            BinaryExpr conditionExpr =
                    (BinaryExpr)
                            doStmt.getCondition();

            boolean satisfiable =
                    z3Analyzer.isSatisfiable(
                            conditionExpr,
                            variables
                    );

            if (!satisfiable) {

                System.out.println(
                        "  Condition can never be true "
                                + "after the first execution."
                );

                System.out.println(
                        "  Loop executes at most once."
                );

            } else {

                System.out.println(
                        "  Repeated execution is possible."
                );
            }
        }
    }

    // ==========================================================
    // FOR LOOP ANALYSIS
    // ==========================================================

    private static void analyzeForLoop(
            ForStmt forStmt,
            Map<String, Integer> variables,
            Z3ConstraintAnalyzer z3Analyzer) {

        int line =
                getLine(forStmt);

        System.out.println(
                "FOR at line "
                        + line
        );

        System.out.println(
                "  Initialization: "
                        + forStmt.getInitialization()
        );

        System.out.println(
                "  Condition: "
                        + forStmt.getCompare()
        );

        System.out.println(
                "  Update: "
                        + forStmt.getUpdate()
        );

        // --------------------------------------------------
        // for(;;)
        // --------------------------------------------------

        if (forStmt.getCompare().isEmpty()) {

            if (!containsBreak(forStmt)) {

                reportLogicalError(
                        line,
                        "for(;;)",
                        "INFINITE_FOR"
                );

                System.out.println(
                        "  Logical Error Detected!"
                );

                System.out.println(
                        "  Error Type: Infinite FOR Loop"
                );

                System.out.println(
                        "  FOR loop has no termination condition."
                );

                System.out.println(
                        "  No break statement found."
                );

            } else {

                System.out.println(
                        "  Infinite-style FOR loop detected."
                );

                System.out.println(
                        "  break statement provides an exit."
                );
            }

            return;
        }

        Expression condition =
                forStmt.getCompare().get();

        String conditionText =
                condition.toString();

        System.out.println(
                "  Known variables: "
                        + variables
        );

        // --------------------------------------------------
        // Constant true
        // --------------------------------------------------

        if (isLiteralTrue(condition)) {

            if (!containsBreak(forStmt)) {

                reportLogicalError(
                        line,
                        conditionText,
                        "INFINITE_FOR"
                );

                System.out.println(
                        "  Logical Error Detected!"
                );

                System.out.println(
                        "  Error Type: Infinite FOR Loop"
                );

                System.out.println(
                        "  FOR condition is always true."
                );

                System.out.println(
                        "  No break statement found."
                );

            } else {

                System.out.println(
                        "  FOR condition is always true."
                );

                System.out.println(
                        "  break statement provides an exit."
                );
            }

            return;
        }

        // --------------------------------------------------
        // Binary condition
        // --------------------------------------------------

        if (condition instanceof BinaryExpr) {

            BinaryExpr binaryExpr =
                    (BinaryExpr) condition;

            boolean satisfiable =
                    z3Analyzer.isSatisfiable(
                            binaryExpr,
                            variables
                    );

            if (!satisfiable) {

                reportLogicalError(
                        line,
                        conditionText,
                        "UNREACHABLE_FOR"
                );

                System.out.println(
                        "  Logical Error Detected!"
                );

                System.out.println(
                        "  Error Type: Unreachable FOR Loop"
                );

                System.out.println(
                        "  Condition '"
                                + conditionText
                                + "' can never be true."
                );

                System.out.println(
                        "  FOR body can never execute."
                );

            } else {

                System.out.println(
                        "  FOR condition: reachable"
                );
            }
        }
    }

    // ==========================================================
    // INFINITE LOOP ANALYSIS
    // ==========================================================

    private static void analyzeInfiniteLoops(
            CompilationUnit cu) {

        int infiniteCount = 0;

        // --------------------------------------------------
        // WHILE(true)
        // --------------------------------------------------

        for (WhileStmt whileStmt
                : cu.findAll(WhileStmt.class)) {

            if (isLiteralTrue(
                    whileStmt.getCondition())) {

                if (!containsBreak(whileStmt)) {

                    infiniteCount++;

                    System.out.println(
                            "Infinite WHILE at line "
                                    + getLine(whileStmt)
                                    + ": while(true)"
                    );
                }
            }
        }

        // --------------------------------------------------
        // DO-WHILE(true)
        // --------------------------------------------------

        for (DoStmt doStmt
                : cu.findAll(DoStmt.class)) {

            if (isLiteralTrue(
                    doStmt.getCondition())) {

                if (!containsBreak(doStmt)) {

                    infiniteCount++;

                    System.out.println(
                            "Infinite DO-WHILE at line "
                                    + getLine(doStmt)
                    );
                }
            }
        }

        // --------------------------------------------------
        // FOR(;;)
        // --------------------------------------------------

        for (ForStmt forStmt
                : cu.findAll(ForStmt.class)) {

            if (forStmt.getCompare().isEmpty()) {

                if (!containsBreak(forStmt)) {

                    infiniteCount++;

                    System.out.println(
                            "Infinite FOR at line "
                                    + getLine(forStmt)
                                    + ": for(;;)"
                    );
                }
            }
        }

        if (infiniteCount == 0) {

            System.out.println(
                    "No definite infinite loops detected."
            );

        } else {

            System.out.println(
                    "Definite infinite loops detected: "
                            + infiniteCount
            );
        }
    }

    // ==========================================================
    // COMPOUND CONDITION ANALYSIS
    // ==========================================================

    private static void analyzeCompoundConditions(
            CompilationUnit cu,
            Map<String, Integer> variables,
            Z3ConstraintAnalyzer z3Analyzer) {

        int compoundCount = 0;

        for (BinaryExpr expression
                : cu.findAll(BinaryExpr.class)) {

            BinaryExpr.Operator operator =
                    expression.getOperator();

            if (operator == BinaryExpr.Operator.AND
                    || operator == BinaryExpr.Operator.OR) {

                compoundCount++;

                int line =
                        getLine(expression);

                System.out.println(
                        "Compound condition at line "
                                + line
                                + ": "
                                + expression
                );

                System.out.println(
                        "  Operator: "
                                + operator
                );

                System.out.println(
                        "  Left condition: "
                                + expression.getLeft()
                );

                System.out.println(
                        "  Right condition: "
                                + expression.getRight()
                );

                BinaryExpr left =
                        getBinaryExpression(
                                expression.getLeft()
                        );

                BinaryExpr right =
                        getBinaryExpression(
                                expression.getRight()
                        );

                if (left != null
                        && right != null) {

                    boolean leftSat =
                            z3Analyzer.isSatisfiable(
                                    left,
                                    variables
                            );

                    boolean rightSat =
                            z3Analyzer.isSatisfiable(
                                    right,
                                    variables
                            );

                    // --------------------------------------------------
                    // AND
                    // --------------------------------------------------

                    if (operator
                            == BinaryExpr.Operator.AND) {

                        if (!leftSat || !rightSat) {

                            System.out.println(
                                    "  One side of AND "
                                            + "is unsatisfiable."
                            );

                        } else {

                            System.out.println(
                                    "  Both sides are "
                                            + "individually reachable."
                            );
                        }

                        /*
                         * Check the COMPLETE AND condition.
                         *
                         * This is important because:
                         *
                         * x > 10 && x < 3
                         *
                         * has individually meaningful comparisons,
                         * but the complete condition is impossible.
                         */

                        boolean completeSat =
                                z3Analyzer.isSatisfiable(
                                        expression,
                                        variables
                                );

                        if (!completeSat) {

                            reportLogicalError(
                                    line,
                                    expression.toString(),
                                    "UNSATISFIABLE_COMPOUND"
                            );

                            System.out.println(
                                    "  Logical Error Detected!"
                            );

                            System.out.println(
                                    "  Error Type: "
                                            + "Contradictory / "
                                            + "Unsatisfiable Compound Condition"
                            );

                            System.out.println(
                                    "  Complete AND condition "
                                            + "can never be true."
                            );
                        }

                    // --------------------------------------------------
                    // OR
                    // --------------------------------------------------

                    } else {

                        boolean completeSat =
                                z3Analyzer.isSatisfiable(
                                        expression,
                                        variables
                                );

                        if (completeSat) {

                            System.out.println(
                                    "  OR condition is reachable."
                            );

                        } else {

                            reportLogicalError(
                                    line,
                                    expression.toString(),
                                    "UNSATISFIABLE_OR"
                            );

                            System.out.println(
                                    "  Logical Error Detected!"
                            );

                            System.out.println(
                                    "  Error Type: "
                                            + "Unsatisfiable OR Condition"
                            );

                            System.out.println(
                                    "  Neither side can be true."
                            );
                        }
                    }
                }
            }
        }

        if (compoundCount == 0) {

            System.out.println(
                    "No compound conditions found."
            );
        }
    }

    // ==========================================================
    // LOGICAL ERROR CLASSIFICATION
    // ==========================================================

    private static void classifyLogicalError(
            IfStmt ifStmt,
            Map<String, Integer> variables,
            String condition) {

        if (!(ifStmt.getCondition()
                instanceof BinaryExpr)) {

            System.out.println(
                    "Error Type: "
                            + "False / Unsatisfiable Condition"
            );

            return;
        }

        BinaryExpr binaryExpr =
                (BinaryExpr)
                        ifStmt.getCondition();

        BinaryExpr.Operator operator =
                binaryExpr.getOperator();

        if (operator
                == BinaryExpr.Operator.EQUALS) {

            System.out.println(
                    "Error Type: "
                            + "Impossible Equality Condition"
            );

        } else if (operator
                == BinaryExpr.Operator.NOT_EQUALS) {

            System.out.println(
                    "Error Type: "
                            + "Impossible Inequality Condition"
            );

        } else if (operator
                == BinaryExpr.Operator.AND) {

            System.out.println(
                    "Error Type: "
                            + "Contradictory / "
                            + "Unsatisfiable Condition"
            );

        } else if (operator
                == BinaryExpr.Operator.GREATER
                || operator
                == BinaryExpr.Operator.GREATER_EQUALS
                || operator
                == BinaryExpr.Operator.LESS
                || operator
                == BinaryExpr.Operator.LESS_EQUALS) {

            System.out.println(
                    "Error Type: "
                            + "Impossible Relational Condition"
            );

        } else {

            System.out.println(
                    "Error Type: "
                            + "False / Unsatisfiable Condition"
            );
        }

        System.out.println(
                "Condition '"
                        + condition
                        + "' is false for "
                        + "the known variable values."
        );
    }

    // ==========================================================
    // CHECK WHETHER CONDITION IS ALWAYS TRUE
    // ==========================================================

    private static boolean isConditionAlwaysTrue(
            BinaryExpr expression,
            Map<String, Integer> variables) {

        Integer leftValue =
                resolveIntegerValue(
                        expression.getLeft(),
                        variables
                );

        Integer rightValue =
                resolveIntegerValue(
                        expression.getRight(),
                        variables
                );

        if (leftValue == null
                || rightValue == null) {

            return false;
        }

        switch (expression.getOperator()) {

            case EQUALS:
                return leftValue.equals(rightValue);

            case NOT_EQUALS:
                return !leftValue.equals(rightValue);

            case GREATER:
                return leftValue > rightValue;

            case GREATER_EQUALS:
                return leftValue >= rightValue;

            case LESS:
                return leftValue < rightValue;

            case LESS_EQUALS:
                return leftValue <= rightValue;

            default:
                return false;
        }
    }

    // ==========================================================
    // RESOLVE INTEGER VALUE
    // ==========================================================

    private static Integer resolveIntegerValue(
            Expression expression,
            Map<String, Integer> variables) {

        if (expression instanceof IntegerLiteralExpr) {

            try {

                return Integer.parseInt(
                        expression.toString()
                );

            } catch (NumberFormatException e) {

                return null;
            }
        }

        if (expression instanceof NameExpr) {

            String variableName =
                    expression.toString();

            return variables.get(
                    variableName
            );
        }

        return null;
    }

    // ==========================================================
    // GET BINARY EXPRESSION
    // ==========================================================

    private static BinaryExpr getBinaryExpression(
            Expression expression) {

        if (expression instanceof BinaryExpr) {

            return (BinaryExpr) expression;
        }

        return null;
    }

    // ==========================================================
    // CHECK LITERAL TRUE
    // ==========================================================

    private static boolean isLiteralTrue(
            Expression expression) {

        return expression.toString()
                .equals("true");
    }

    // ==========================================================
    // CHECK LITERAL FALSE
    // ==========================================================

    private static boolean isLiteralFalse(
            Expression expression) {

        return expression.toString()
                .equals("false");
    }

    // ==========================================================
    // CHECK WHETHER LOOP HAS BREAK
    // ==========================================================

    private static boolean containsBreak(
            WhileStmt whileStmt) {

        return !whileStmt
                .findAll(BreakStmt.class)
                .isEmpty();
    }

    private static boolean containsBreak(
            DoStmt doStmt) {

        return !doStmt
                .findAll(BreakStmt.class)
                .isEmpty();
    }

    private static boolean containsBreak(
            ForStmt forStmt) {

        return !forStmt
                .findAll(BreakStmt.class)
                .isEmpty();
    }

    // ==========================================================
    // GET SOURCE LINE
    // ==========================================================

    private static int getLine(
            com.github.javaparser.ast.Node node) {

        return node.getBegin()
                .map(position -> position.line)
                .orElse(-1);
    }
}