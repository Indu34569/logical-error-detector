package com.project.analyzer;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.expr.Expression;

public class ConstraintZ3IntegrationTest {

    public static void main(String[] args) {

        // ---------------------------------------------
        // Create the condition using JavaParser
        // ---------------------------------------------

        Expression expression =
                StaticJavaParser.parseExpression("x > 10");

        // ---------------------------------------------
        // Generate constraint
        // ---------------------------------------------

        ConstraintGenerator generator =
                new ConstraintGenerator();

        String constraint =
                generator.generateConstraint(expression);

        System.out.println("Generated Constraint:");
        System.out.println(constraint);

        // ---------------------------------------------
        // Test with known value x = 5
        // ---------------------------------------------

        Z3ConstraintAnalyzer analyzer =
                new Z3ConstraintAnalyzer();

        boolean result =
                analyzer.isSatisfiable(5, ">", 10);

        System.out.println();
        System.out.println("Z3 Result:");

        if (result) {
            System.out.println("SATISFIABLE");
            System.out.println(
                    "Condition is possible."
            );
        } else {
            System.out.println("UNSATISFIABLE");
            System.out.println(
                    "Logical Error Detected!"
            );
        }

        analyzer.close();
    }
}