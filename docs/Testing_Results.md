# Testing Results

| Test Case | Input | Expected Result | Actual Result | Status |
|---|---|---|---|---|
| 1 | Benchmark100.java | Detect logical errors | Logical errors detected | PASS |
| 2 | Benchmark200.java | Detect logical errors | 15 unique logical errors detected | PASS |
| 3 | Syntax validation | Detect syntax errors | No syntax errors found | PASS |
| 4 | Division by zero | Detect zero divisor | Detected | PASS |
| 5 | Infinite loop | Detect always-true loop | Detected | PASS |
| 6 | Constant condition | Detect constant IF | Detected | PASS |
| 7 | Dead assignment | Detect overwritten value | Detected | PASS |
| 8 | Z3 analysis | Verify constraints | 7 conditions checked | PASS |
| 9 | Java 17 compilation | Build successfully | BUILD SUCCESS | PASS |
| 10 | Git verification | Repository synchronized | Working tree clean | PASS |