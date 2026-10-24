package com.project.analyzer;

import com.microsoft.z3.*;

public class Z3Test {

    public static void main(String[] args) {

        // Create Z3 context
        try (Context ctx = new Context()) {

            // Create an integer variable
            IntExpr x = ctx.mkIntConst("x");

            // Create solver
            Solver solver = ctx.mkSolver();

            // Add constraints:
            // x = 5
            // x > 10
            solver.add(ctx.mkEq(x, ctx.mkInt(5)));
            solver.add(ctx.mkGt(x, ctx.mkInt(10)));

            // Check whether constraints are satisfiable
            Status result = solver.check();

            System.out.println("Z3 Result: " + result);

            if (result == Status.UNSATISFIABLE) {
                System.out.println(
                        "Contradiction detected by Z3!"
                );
            }
        }
    }
}