package com.project.analyzer;

import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.microsoft.z3.*;

import java.util.Map;

public class Z3ConstraintAnalyzer {

    private final Context context;

    public Z3ConstraintAnalyzer() {
        context = new Context();
    }

    // --------------------------------------------------
    // Backward-compatible simple constraint analysis
    // --------------------------------------------------

    public boolean isSatisfiable(
            int variableValue,
            String operator,
            int comparisonValue) {

        Solver solver = context.mkSolver();

        IntExpr x =
                context.mkIntConst("x");

        BoolExpr variableConstraint =
                context.mkEq(
                        x,
                        context.mkInt(variableValue)
                );

        BoolExpr condition;

        switch (operator) {

            case ">":
                condition =
                        context.mkGt(
                                x,
                                context.mkInt(comparisonValue)
                        );
                break;

            case ">=":
                condition =
                        context.mkGe(
                                x,
                                context.mkInt(comparisonValue)
                        );
                break;

            case "<":
                condition =
                        context.mkLt(
                                x,
                                context.mkInt(comparisonValue)
                        );
                break;

            case "<=":
                condition =
                        context.mkLe(
                                x,
                                context.mkInt(comparisonValue)
                        );
                break;

            case "==":
                condition =
                        context.mkEq(
                                x,
                                context.mkInt(comparisonValue)
                        );
                break;

            case "!=":
                condition =
                        context.mkDistinct(
                                x,
                                context.mkInt(comparisonValue)
                        );
                break;

            default:
                throw new IllegalArgumentException(
                        "Unsupported operator: " + operator
                );
        }

        solver.add(variableConstraint);
        solver.add(condition);

        Status result =
                solver.check();
	System.out.println("Z3 Solver Result: " + result);

        return result == Status.SATISFIABLE;
    }

    // --------------------------------------------------
    // Analyze a complete JavaParser expression using Z3
    // --------------------------------------------------

    public boolean isSatisfiable(
            Expression expression,
            Map<String, Integer> variables) {

        Solver solver = context.mkSolver();

        BoolExpr constraint =
                buildConstraint(
                        expression,
                        variables
                );

        solver.add(constraint);

        Status result =
                solver.check();

        return result == Status.SATISFIABLE;
    }

    // --------------------------------------------------
    // Build Z3 constraint
    // --------------------------------------------------

    private BoolExpr buildConstraint(
            Expression expression,
            Map<String, Integer> variables) {

        // --------------------------------------------------
        // Binary expression
        // --------------------------------------------------

        if (expression instanceof BinaryExpr) {

            BinaryExpr binary =
                    (BinaryExpr) expression;

            // --------------------------------------------------
            // AND
            // --------------------------------------------------

            if (binary.getOperator()
                    == BinaryExpr.Operator.AND) {

                BoolExpr left =
                        buildConstraint(
                                binary.getLeft(),
                                variables
                        );

                BoolExpr right =
                        buildConstraint(
                                binary.getRight(),
                                variables
                        );

                return context.mkAnd(left, right);
            }

            // --------------------------------------------------
            // OR
            // --------------------------------------------------

            if (binary.getOperator()
                    == BinaryExpr.Operator.OR) {

                BoolExpr left =
                        buildConstraint(
                                binary.getLeft(),
                                variables
                        );

                BoolExpr right =
                        buildConstraint(
                                binary.getRight(),
                                variables
                        );

                return context.mkOr(left, right);
            }

            // --------------------------------------------------
            // Normal comparison
            // --------------------------------------------------

            ArithExpr left =
                    getValue(
                            binary.getLeft(),
                            variables
                    );

            ArithExpr right =
                    getValue(
                            binary.getRight(),
                            variables
                    );

            // Unknown values cannot be analyzed yet
            if (left == null || right == null) {
                return context.mkTrue();
            }

            switch (binary.getOperator()) {

                case GREATER:
                    return context.mkGt(left, right);

                case GREATER_EQUALS:
                    return context.mkGe(left, right);

                case LESS:
                    return context.mkLt(left, right);

                case LESS_EQUALS:
                    return context.mkLe(left, right);

                case EQUALS:
                    return context.mkEq(left, right);

                case NOT_EQUALS:
                    return context.mkDistinct(left, right);

                default:
                    return context.mkTrue();
            }
        }

        // --------------------------------------------------
        // Unknown expression
        // --------------------------------------------------

        return context.mkTrue();
    }

    // --------------------------------------------------
    // Convert Java expression into Z3 integer expression
    // --------------------------------------------------

    private ArithExpr getValue(
            Expression expression,
            Map<String, Integer> variables) {

        // --------------------------------------------------
        // Integer literal
        // --------------------------------------------------

        if (expression instanceof IntegerLiteralExpr) {

            int value =
                    Integer.parseInt(
                            expression.toString()
                    );

            return context.mkInt(value);
        }

        // --------------------------------------------------
        // Variable
        // --------------------------------------------------

        if (expression instanceof NameExpr) {

            String variableName =
                    expression.toString();

            Integer value =
                    variables.get(variableName);

            if (value == null) {
                return null;
            }

            return context.mkInt(value);
        }

        return null;
    }

    // --------------------------------------------------
    // Close Z3 context
    // --------------------------------------------------

    public void close() {
        context.close();
    }
}