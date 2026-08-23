package com.project.analyzer;

public class Z3ConstraintAnalyzerTest {

    public static void main(String[] args) {

        Z3ConstraintAnalyzer analyzer =
                new Z3ConstraintAnalyzer();

        System.out.println("Test 1: x = 5, x > 10");

        boolean result1 =
                analyzer.isSatisfiable(5, ">", 10);

        System.out.println(
                "Satisfiable: " + result1
        );

        System.out.println();

        System.out.println("Test 2: x = 5, x < 10");

        boolean result2 =
                analyzer.isSatisfiable(5, "<", 10);

        System.out.println(
                "Satisfiable: " + result2
        );

        analyzer.close();
    }
}