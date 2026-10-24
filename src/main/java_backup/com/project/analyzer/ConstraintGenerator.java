package com.project.analyzer;

import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;

public class ConstraintGenerator {

    // --------------------------------------------------
    // Generate a constraint from a Java expression
    // --------------------------------------------------

    public String generateConstraint(Expression expression) {

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

                String leftConstraint =
                        generateConstraint(
                                binary.getLeft()
                        );

                String rightConstraint =
                        generateConstraint(
                                binary.getRight()
                        );

                return "("
                        + leftConstraint
                        + " AND "
                        + rightConstraint
                        + ")";
            }

            // --------------------------------------------------
            // OR
            // --------------------------------------------------

            if (binary.getOperator()
                    == BinaryExpr.Operator.OR) {

                String leftConstraint =
                        generateConstraint(
                                binary.getLeft()
                        );

                String rightConstraint =
                        generateConstraint(
                                binary.getRight()
                        );

                return "("
                        + leftConstraint
                        + " OR "
                        + rightConstraint
                        + ")";
            }

            // --------------------------------------------------
            // Normal comparison
            // --------------------------------------------------

            return binary.getLeft().toString()
                    + " "
                    + getOperatorSymbol(
                            binary.getOperator()
                    )
                    + " "
                    + binary.getRight().toString();
        }

        // --------------------------------------------------
        // Unknown expression
        // --------------------------------------------------

        return expression.toString();
    }

    // --------------------------------------------------
    // Convert JavaParser operator to constraint symbol
    // --------------------------------------------------

    private String getOperatorSymbol(
            BinaryExpr.Operator operator) {

        switch (operator) {

            case GREATER:
                return ">";

            case GREATER_EQUALS:
                return ">=";

            case LESS:
                return "<";

            case LESS_EQUALS:
                return "<=";

            case EQUALS:
                return "==";

            case NOT_EQUALS:
                return "!=";

            default:
                return operator.asString();
        }
    }
}