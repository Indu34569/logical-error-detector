# Static Analysis Tool to Detect Logical Errors in Procedural Programs

## 📌 Project Overview

The **Static Analysis Tool to Detect Logical Errors in Procedural Programs** is a Java-based static analysis project developed to identify common logical errors in source code without executing the program.

The tool analyzes a Java program, builds constraints from program conditions, and uses the **Z3 SMT Solver** to determine whether conditions are satisfiable, contradictory, or unreachable.

## 🎯 Objectives

* Analyze Java source code using static analysis techniques.
* Detect conditions that can never be true.
* Identify unreachable branches and loops.
* Detect contradictory or unsatisfiable conditions.
* Use program-order data-flow information to track known variable values.
* Use Z3 to perform constraint-based logical reasoning.
* Provide a clear analysis report to the user.

## 🔍 Logical Errors Detected

The current implementation can detect examples such as:

### 1. Impossible Equality

```java
int k = 0;

if (k == 5) {
    System.out.println("k is 5");
}
```

The condition can never be true because `k` is known to be `0`.

### 2. Unreachable FOR Loop

```java
int j = 10;

for (j = 10; j < 5; j++) {
    System.out.println("This loop cannot execute");
}
```

The loop condition is false before the first iteration.

### 3. Infinite WHILE Loop

```java
while (true) {
    System.out.println("This loop never ends");
}
```

The condition is always true and no `break` statement is present.

### 4. Contradictory Compound Condition

```java
int x = 5;

if (x > 10 && x < 3) {
    System.out.println("This condition is impossible");
}
```

Both conditions cannot be satisfied simultaneously.

### 5. Unreachable WHILE Loop

```java
int b = 10;

while (b < 5) {
    System.out.println("This loop cannot execute");
}
```

The loop condition is false with the known value of `b`.

### 6. Valid Condition

```java
int x = 5;

if (x < 10) {
    System.out.println("Condition is valid");
}
```

This condition is satisfiable, so no logical error is reported.

## 🏗️ Project Architecture

```text
Java Source Program
        ↓
JavaParser
        ↓
Syntax Analysis
        ↓
Program-Order Data-Flow Analysis
        ↓
Condition Analysis
        ↓
Constraint Generation
        ↓
Z3 SMT Solver
        ↓
Logical Error Detection
        ↓
Final Analysis Report
```

## 🛠️ Technologies Used

* **Java 17**
* **JavaParser**
* **Z3 SMT Solver**
* **Apache Maven**
* **Git / GitHub**

All tools and libraries used in this project are free to use.

## 📂 Project Structure

```text
logical-error-detector/
│
├── pom.xml
├── .gitignore
│
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── project/
│                   └── analyzer/
│                       ├── ConditionAnalyzer.java
│                       ├── ConstraintGenerator.java
│                       ├── LogicalErrorDetector.java
│                       └── Z3ConstraintAnalyzer.java
│
└── test-input/
    ├── TestProgram.java
    └── tests/
        ├── TestImpossibleEquality.java
        ├── TestUnreachableFor.java
        ├── TestInfiniteLoop.java
        ├── TestContradictoryCondition.java
        ├── TestValidCondition.java
        └── TestUnreachableWhile.java
```

## ▶️ How to Build

Make sure Java 17 and Maven are installed.

Clone the repository:

```bash
git clone https://github.com/Indu34569/logical-error-detector.git
```

Enter the project directory:

```bash
cd logical-error-detector
```

Compile the project:

```bash
mvn clean compile
```

## ▶️ Running the Analyzer

The analyzer can be run using the compiled classes together with the required JavaParser and Z3 dependencies.

The input Java source file is configured in:

```text
src/main/java/com/project/analyzer/LogicalErrorDetector.java
```

The tool produces a structured report containing:

* Syntax analysis
* Data-flow information
* Condition analysis
* Branch analysis
* Loop analysis
* Logical error detection
* Final error count

## 🧪 Testing

The project contains multiple test programs covering different logical situations, including:

* Impossible conditions
* Unreachable branches
* Unreachable loops
* Infinite loops
* Contradictory compound conditions
* Valid conditions

These test programs are located in:

```text
test-input/tests/
```

## 📊 Example Result

For a contradictory condition such as:

```java
int x = 5;

if (x > 10 && x < 3) {
    System.out.println("Impossible");
}
```

the analyzer reports that the condition is **UNSATISFIABLE** and identifies the corresponding logical error.

For a valid condition such as:

```java
int x = 5;

if (x < 10) {
    System.out.println("Condition is valid");
}
```

the analyzer reports:

```text
✓ No logical errors detected.
```

## 🚀 Future Enhancements

Possible future improvements include:

* Detecting additional logical error patterns.
* Improving data-flow analysis.
* Supporting more complex expressions.
* Improving handling of variable assignments.
* Adding more loop and branch analysis.
* Improving the user interface and analysis report.
* Expanding automated test coverage.

## 👩‍💻 Project

**Project:** Static Analysis Tool to Detect Logical Errors in Procedural Programs

**Repository:** `logical-error-detector`

**Platform:** Java / Maven

**Purpose:** Academic / Final-Year Project
