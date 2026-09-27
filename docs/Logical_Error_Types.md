# Logical Error Types

| No. | Error Type | Description | Detection Method |
|---|---|---|---|
| 1 | Impossible IF Condition | IF condition can never be true | Data-flow + Z3 |
| 2 | Impossible Loop Condition | Loop condition can never be true | Data-flow + Z3 |
| 3 | Division By Zero | A divisor evaluates to zero | Static analysis |
| 4 | Constant Condition | Condition always evaluates to the same result | Constant analysis + Z3 |
| 5 | Potential Infinite Loop | Loop condition is always true | Loop analysis |
| 6 | Missing Loop Update | Loop control variable is not updated | Data-flow analysis |
| 7 | Unreachable Code | Code cannot be reached during execution | Control-flow analysis |
| 8 | Uninitialized Variable | Variable may be used before initialization | Data-flow analysis |
| 9 | Dead Assignment | Assigned value is overwritten before use | Data-flow analysis |
| 10 | Z3 Unsatisfiable Condition | Z3 proves a condition cannot be satisfied | Z3 SMT solver |
| 11 | Z3 Always-True Condition | Z3 proves a condition is always true | Z3 SMT solver |