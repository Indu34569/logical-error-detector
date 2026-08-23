package com.project.analyzer;

import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.NameExpr;

import java.util.Map;

public class ConditionAnalyzer {

    // --------------------------------------------------
    // Analyze a complete condition
    // --------------------------------------------------

    public boolean evaluateExpression(
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
            // Example:
            // x > 10 && x < 20
            // --------------------------------------------------

            if (binary.getOperator()
                    == BinaryExpr.Operator.AND) {

                boolean leftResult =
                        evaluateExpression(
                                binary.getLeft(),
                                variables
                        );

                boolean rightResult =
                        evaluateExpression(
                                binary.getRight(),
                                variables
                        );

                return leftResult && rightResult;
            }

            // --------------------------------------------------
            // OR
            // Example:
            // x > 10 || x < 3
            // --------------------------------------------------

            if (binary.getOperator()
                    == BinaryExpr.Operator.OR) {

                boolean leftResult =
                        evaluateExpression(
                                binary.getLeft(),
                                variables
                        );

                boolean rightResult =
                        evaluateExpression(
                                binary.getRight(),
                                variables
                        );

                return leftResult || rightResult;
            }

            // --------------------------------------------------
            // Normal comparison
            // --------------------------------------------------

            Integer leftValue =
                    getValue(
                            binary.getLeft(),
                            variables
                    );

            Integer rightValue =
                    getValue(
                            binary.getRight(),
                            variables
                    );

            // Unknown values cannot be analyzed yet
            if (leftValue == null
                    || rightValue == null) {

                return true;
            }

            return evaluateCondition(
                    leftValue,
                    rightValue,
                    binary.getOperator()
            );
        }

        // Unknown expression
        return true;
    }

    // --------------------------------------------------
    // Get value from expression
    // --------------------------------------------------

    private Integer getValue(
            Expression expression,
            Map<String, Integer> variables) {

        // Integer value
        if (expression instanceof IntegerLiteralExpr) {

            return Integer.parseInt(
                    expression.toString()
            );
        }

        // Variable value
        if (expression instanceof NameExpr) {

            String variableName =
                    expression.toString();

            return variables.get(variableName);
        }

        return null;
    }

    // --------------------------------------------------
    // Evaluate comparison
    // --------------------------------------------------

    private boolean evaluateCondition(
            int left,
            int right,
            BinaryExpr.Operator operator) {

        switch (operator) {

            case GREATER:
                return left > right;

            case GREATER_EQUALS:
                return left >= right;

            case LESS:
                return left < right;

            case LESS_EQUALS:
                return left <= right;

            case EQUALS:
                return left == right;

            case NOT_EQUALS:
                return left != right;

            default:
                return true;
        }
    }
}