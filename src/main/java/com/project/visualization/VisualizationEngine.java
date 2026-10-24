package com.project.visualization;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VisualizationEngine {

    private static final int MAX_ITERATIONS = 10000;

    private VisualizationEngine() {
    }

    // ============================================================
    // CREATE EVENTS
    // ============================================================

    public static List<VisualizationEvent> createEvents(
            String code) {

        List<VisualizationEvent> events =
                new ArrayList<>();

        Map<String, Double> variables =
                new LinkedHashMap<>();

        if (code == null || code.isBlank()) {
            return events;
        }

        String[] lines =
                code.split("\\R", -1);

        executeRange(
                lines,
                0,
                lines.length - 1,
                variables,
                events
        );

        return events;
    }

    // ============================================================
    // EXECUTE RANGE
    // ============================================================

    private static void executeRange(
            String[] lines,
            int start,
            int end,
            Map<String, Double> variables,
            List<VisualizationEvent> events) {

        int i = start;

        while (i <= end) {

            String line =
                    lines[i].trim();

            if (line.isEmpty()) {
                i++;
                continue;
            }

            // ----------------------------------------------------
            // WHILE
            // ----------------------------------------------------

            if (isWhile(line)) {

                int close =
                        findMatchingBrace(
                                lines,
                                i,
                                end
                        );

                if (close >= 0) {

                    executeWhile(
                            lines,
                            i,
                            close,
                            line,
                            variables,
                            events
                    );

                    i = close + 1;
                    continue;
                }
            }

            // ----------------------------------------------------
            // FOR
            // ----------------------------------------------------

            if (isFor(line)) {

                int close =
                        findMatchingBrace(
                                lines,
                                i,
                                end
                        );

                if (close >= 0) {

                    executeFor(
                            lines,
                            i,
                            close,
                            line,
                            variables,
                            events
                    );

                    i = close + 1;
                    continue;
                }
            }

            // ----------------------------------------------------
            // IF
            // ----------------------------------------------------

            if (isIf(line)) {

                int close =
                        findMatchingBrace(
                                lines,
                                i,
                                end
                        );

                if (close >= 0) {

                    String condition =
                            extractCondition(line);

                    boolean result =
                            evaluateCondition(
                                    condition,
                                    variables
                            );

                    add(
                            events,
                            "SOURCE",
                            line,
                            i + 1
                    );

                    add(
                            events,
                            "CONDITION",
                            condition + " -> " + result,
                            i + 1
                    );

                    if (result) {

                        executeRange(
                                lines,
                                i + 1,
                                close - 1,
                                variables,
                                events
                        );
                    }

                    i = close + 1;
                    continue;
                }
            }

            // ----------------------------------------------------
            // NORMAL STATEMENT
            // ----------------------------------------------------

            executeStatement(
                    line,
                    i + 1,
                    variables,
                    events
            );

            i++;
        }
    }

    // ============================================================
    // WHILE
    // ============================================================

    private static void executeWhile(
            String[] lines,
            int loopLine,
            int close,
            String loopText,
            Map<String, Double> variables,
            List<VisualizationEvent> events) {

        String condition =
                extractCondition(loopText);

        add(
                events,
                "SOURCE",
                loopText,
                loopLine + 1
        );

        int iteration = 0;

        while (iteration < MAX_ITERATIONS) {

            boolean conditionResult =
                    evaluateCondition(
                            condition,
                            variables
                    );

            add(
                    events,
                    "CONDITION",
                    condition + " -> " + conditionResult,
                    loopLine + 1
            );

            if (!conditionResult) {

                add(
                        events,
                        "LOOP",
                        "LOOP STOPPED after "
                                + iteration
                                + " iteration(s)",
                        loopLine + 1
                );

                return;
            }

            iteration++;

            add(
                    events,
                    "LOOP",
                    "LOOP ITERATION "
                            + iteration,
                    loopLine + 1
            );

            executeRange(
                    lines,
                    loopLine + 1,
                    close - 1,
                    variables,
                    events
            );
        }

        add(
                events,
                "LOOP",
                "LOOP STOPPED after "
                        + iteration
                        + " iteration(s) - MAXIMUM LIMIT REACHED",
                loopLine + 1
        );
    }

    // ============================================================
    // FOR
    // ============================================================

    private static void executeFor(
            String[] lines,
            int loopLine,
            int close,
            String loopText,
            Map<String, Double> variables,
            List<VisualizationEvent> events) {

        String inside =
                extractCondition(loopText);

        String[] parts =
                inside.split(";", -1);

        if (parts.length != 3) {

            add(
                    events,
                    "SOURCE",
                    loopText,
                    loopLine + 1
            );

            return;
        }

        String initialization =
                parts[0].trim();

        String condition =
                parts[1].trim();

        String update =
                parts[2].trim();

        add(
                events,
                "SOURCE",
                loopText,
                loopLine + 1
        );

        if (!initialization.isBlank()) {

            executeStatement(
                    initialization,
                    loopLine + 1,
                    variables,
                    events
            );
        }

        int iteration = 0;

        while (iteration < MAX_ITERATIONS) {

            boolean conditionResult =
                    evaluateCondition(
                            condition,
                            variables
                    );

            add(
                    events,
                    "CONDITION",
                    condition + " -> " + conditionResult,
                    loopLine + 1
            );

            if (!conditionResult) {

                add(
                        events,
                        "LOOP",
                        "LOOP STOPPED after "
                                + iteration
                                + " iteration(s)",
                        loopLine + 1
                );

                return;
            }

            iteration++;

            add(
                    events,
                    "LOOP",
                    "LOOP ITERATION "
                            + iteration,
                    loopLine + 1
            );

            executeRange(
                    lines,
                    loopLine + 1,
                    close - 1,
                    variables,
                    events
            );

            if (!update.isBlank()) {

                executeStatement(
                        update,
                        loopLine + 1,
                        variables,
                        events
                );
            }
        }

        add(
                events,
                "LOOP",
                "LOOP STOPPED after "
                        + iteration
                        + " iteration(s) - MAXIMUM LIMIT REACHED",
                loopLine + 1
        );
    }

    // ============================================================
    // STATEMENT
    // ============================================================

    private static void executeStatement(
            String line,
            int lineNumber,
            Map<String, Double> variables,
            List<VisualizationEvent> events) {

        if (line == null || line.isBlank()) {
            return;
        }

        line =
                line.replace("{", "")
                        .replace("}", "")
                        .trim();

        if (line.isBlank()) {
            return;
        }

        add(
                events,
                "SOURCE",
                line,
                lineNumber
        );

        processVariable(
                line,
                lineNumber,
                variables,
                events
        );

        extractOperators(
                line,
                lineNumber,
                events
        );
    }

    // ============================================================
    // VARIABLE PROCESSING
    // ============================================================

    private static void processVariable(
            String line,
            int lineNumber,
            Map<String, Double> variables,
            List<VisualizationEvent> events) {

        // --------------------------------------------------------
        // DECLARATION
        // --------------------------------------------------------

        Matcher declaration =
                Pattern.compile(
                        "\\b(int|double|float|long|short|byte|boolean|char|String)"
                                + "\\s+([A-Za-z_$][\\w$]*)"
                                + "(?:\\s*=\\s*([^;]+))?"
                ).matcher(line);

        if (declaration.find()) {

            String name =
                    declaration.group(2);

            String value =
                    declaration.group(3);

            double number = 0;

            if (value != null) {

                Double evaluated =
                        evaluateNumber(
                                value,
                                variables
                        );

                if (evaluated != null) {
                    number = evaluated;
                }
            }

            variables.put(
                    name,
                    number
            );

            add(
                    events,
                    "VARIABLE",
                    name + " = " + format(number),
                    lineNumber
            );

            return;
        }

        // --------------------------------------------------------
        // ++ / --
        // --------------------------------------------------------

        String cleaned =
                removeSemicolon(line);

        Matcher increment =
                Pattern.compile(
                        "^(?:\\+\\+)?"
                                + "([A-Za-z_$][\\w$]*)"
                                + "(\\+\\+|--)$"
                ).matcher(cleaned);

        if (increment.find()) {

            updateIncrement(
                    increment.group(1),
                    increment.group(2),
                    lineNumber,
                    variables,
                    events
            );

            return;
        }

        // --------------------------------------------------------
        // ASSIGNMENT
        // --------------------------------------------------------

        Matcher assignment =
                Pattern.compile(
                        "^([A-Za-z_$][\\w$]*)\\s*"
                                + "(=|\\+=|-=|\\*=|/=|%=)"
                                + "\\s*(.+?)\\s*;?$"
                ).matcher(line);

        if (!assignment.find()) {
            return;
        }

        String name =
                assignment.group(1);

        String operator =
                assignment.group(2);

        Double right =
                evaluateNumber(
                        assignment.group(3),
                        variables
                );

        if (right == null) {
            return;
        }

        double oldValue =
                variables.getOrDefault(
                        name,
                        0.0
                );

        double newValue;

        switch (operator) {

            case "=":
                newValue = right;
                break;

            case "+=":
                newValue = oldValue + right;
                break;

            case "-=":
                newValue = oldValue - right;
                break;

            case "*=":
                newValue = oldValue * right;
                break;

            case "/=":

                if (right == 0) {
                    return;
                }

                newValue =
                        oldValue / right;

                break;

            case "%=":

                if (right == 0) {
                    return;
                }

                newValue =
                        oldValue % right;

                break;

            default:
                return;
        }

        variables.put(
                name,
                newValue
        );

        add(
                events,
                "VARIABLE",
                name
                        + ": "
                        + format(oldValue)
                        + " → "
                        + format(newValue),
                lineNumber
        );
    }

    // ============================================================
    // INCREMENT
    // ============================================================

    private static void updateIncrement(
            String name,
            String operator,
            int lineNumber,
            Map<String, Double> variables,
            List<VisualizationEvent> events) {

        double oldValue =
                variables.getOrDefault(
                        name,
                        0.0
                );

        double newValue =
                operator.equals("++")
                        ? oldValue + 1
                        : oldValue - 1;

        variables.put(
                name,
                newValue
        );

        add(
                events,
                "VARIABLE",
                name
                        + ": "
                        + format(oldValue)
                        + " → "
                        + format(newValue),
                lineNumber
        );
    }

    // ============================================================
    // CONDITION
    // ============================================================

    private static boolean evaluateCondition(
            String condition,
            Map<String, Double> variables) {

        if (condition == null ||
                condition.isBlank()) {

            return false;
        }

        String c =
                condition.trim();

        while (hasOuterParentheses(c)) {

            c =
                    c.substring(
                            1,
                            c.length() - 1
                    ).trim();
        }

        List<String> andParts =
                splitLogical(c, "&&");

        if (andParts.size() > 1) {

            for (String part : andParts) {

                if (!evaluateCondition(
                        part,
                        variables)) {

                    return false;
                }
            }

            return true;
        }

        List<String> orParts =
                splitLogical(c, "||");

        if (orParts.size() > 1) {

            for (String part : orParts) {

                if (evaluateCondition(
                        part,
                        variables)) {

                    return true;
                }
            }

            return false;
        }

        Matcher matcher =
                Pattern.compile(
                        "^(.+?)\\s*"
                                + "(==|!=|<=|>=|<|>)"
                                + "\\s*(.+)$"
                ).matcher(c);

        if (!matcher.find()) {
            return false;
        }

        Double left =
                evaluateNumber(
                        matcher.group(1),
                        variables
                );

        Double right =
                evaluateNumber(
                        matcher.group(3),
                        variables
                );

        if (left == null ||
                right == null) {

            return false;
        }

        switch (matcher.group(2)) {

            case "==":
                return Double.compare(
                        left,
                        right
                ) == 0;

            case "!=":
                return Double.compare(
                        left,
                        right
                ) != 0;

            case "<":
                return left < right;

            case ">":
                return left > right;

            case "<=":
                return left <= right;

            case ">=":
                return left >= right;

            default:
                return false;
        }
    }

    // ============================================================
    // NUMBER EVALUATION
    // ============================================================

    private static Double evaluateNumber(
            String expression,
            Map<String, Double> variables) {

        if (expression == null) {
            return null;
        }

        String e =
                removeSemicolon(expression);

        if (e.isEmpty()) {
            return null;
        }

        while (hasOuterParentheses(e)) {

            e =
                    e.substring(
                            1,
                            e.length() - 1
                    ).trim();
        }

        if (variables.containsKey(e)) {
            return variables.get(e);
        }

        try {
            return Double.parseDouble(e);
        } catch (NumberFormatException ignored) {
        }

        int operatorIndex =
                findMainOperator(e);

        if (operatorIndex < 0) {
            return null;
        }

        char operator =
                e.charAt(operatorIndex);

        String leftExpression =
                e.substring(
                        0,
                        operatorIndex
                ).trim();

        String rightExpression =
                e.substring(
                        operatorIndex + 1
                ).trim();

        Double left =
                evaluateNumber(
                        leftExpression,
                        variables
                );

        Double right =
                evaluateNumber(
                        rightExpression,
                        variables
                );

        if (left == null ||
                right == null) {

            return null;
        }

        switch (operator) {

            case '+':
                return left + right;

            case '-':
                return left - right;

            case '*':
                return left * right;

            case '/':
                return right == 0
                        ? null
                        : left / right;

            case '%':
                return right == 0
                        ? null
                        : left % right;

            default:
                return null;
        }
    }

    // ============================================================
    // MAIN ARITHMETIC OPERATOR
    // ============================================================

    private static int findMainOperator(
            String expression) {

        int depth = 0;

        for (int i = expression.length() - 1;
             i >= 0;
             i--) {

            char c =
                    expression.charAt(i);

            if (c == ')') {
                depth++;
                continue;
            }

            if (c == '(') {
                depth--;
                continue;
            }

            if (depth != 0) {
                continue;
            }

            if (c == '+' || c == '-') {

                if (i == 0) {
                    continue;
                }

                return i;
            }
        }

        depth = 0;

        for (int i = expression.length() - 1;
             i >= 0;
             i--) {

            char c =
                    expression.charAt(i);

            if (c == ')') {
                depth++;
                continue;
            }

            if (c == '(') {
                depth--;
                continue;
            }

            if (depth != 0) {
                continue;
            }

            if (c == '*' ||
                    c == '/' ||
                    c == '%') {

                return i;
            }
        }

        return -1;
    }

    // ============================================================
    // LOGICAL SPLIT
    // ============================================================

    private static List<String> splitLogical(
            String expression,
            String operator) {

        List<String> parts =
                new ArrayList<>();

        int depth = 0;
        int start = 0;

        for (int i = 0;
             i <= expression.length()
                     - operator.length();
             i++) {

            char c =
                    expression.charAt(i);

            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            }

            if (depth == 0 &&
                    expression.startsWith(
                            operator,
                            i
                    )) {

                parts.add(
                        expression.substring(
                                start,
                                i
                        ).trim()
                );

                i += operator.length() - 1;

                start = i + 1;
            }
        }

        if (start == 0) {
            parts.add(expression);
        } else {
            parts.add(
                    expression.substring(
                            start
                    ).trim()
            );
        }

        return parts;
    }

    // ============================================================
    // OUTER PARENTHESES
    // ============================================================

    private static boolean hasOuterParentheses(
            String value) {

        if (value == null ||
                value.length() < 2 ||
                value.charAt(0) != '(' ||
                value.charAt(value.length() - 1) != ')') {

            return false;
        }

        int depth = 0;

        for (int i = 0;
             i < value.length();
             i++) {

            char c =
                    value.charAt(i);

            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            }

            if (depth == 0 &&
                    i < value.length() - 1) {

                return false;
            }
        }

        return depth == 0;
    }

    // ============================================================
    // REMOVE SEMICOLON
    // ============================================================

    private static String removeSemicolon(
            String value) {

        if (value == null) {
            return "";
        }

        return value
                .trim()
                .replaceAll(";+$", "")
                .trim();
    }

    // ============================================================
    // LOOP DETECTION
    // ============================================================

    private static boolean isWhile(
            String line) {

        return line.matches(
                "^while\\s*\\(.*"
        );
    }

    private static boolean isFor(
            String line) {

        return line.matches(
                "^for\\s*\\(.*"
        );
    }

    private static boolean isIf(
            String line) {

        return line.matches(
                "^if\\s*\\(.*"
        );
    }

    // ============================================================
    // EXTRACT PARENTHESES
    // ============================================================

    private static String extractCondition(
            String line) {

        int start =
                line.indexOf('(');

        if (start < 0) {
            return "";
        }

        int depth = 0;

        for (int i = start;
             i < line.length();
             i++) {

            char c =
                    line.charAt(i);

            if (c == '(') {

                depth++;

            } else if (c == ')') {

                depth--;

                if (depth == 0) {

                    return line.substring(
                            start + 1,
                            i
                    ).trim();
                }
            }
        }

        return "";
    }

    // ============================================================
    // MATCH BRACE
    // ============================================================

    private static int findMatchingBrace(
            String[] lines,
            int start,
            int end) {

        int depth = 0;
        boolean opened = false;

        for (int i = start;
             i <= end;
             i++) {

            for (char c : lines[i].toCharArray()) {

                if (c == '{') {

                    depth++;
                    opened = true;

                } else if (c == '}') {

                    depth--;

                    if (opened &&
                            depth == 0) {

                        return i;
                    }
                }
            }
        }

        return -1;
    }

    // ============================================================
    // OPERATORS
    // ============================================================

    private static void extractOperators(
            String line,
            int lineNumber,
            List<VisualizationEvent> events) {

        Matcher matcher =
                Pattern.compile(
                        "(\\+\\+|--|==|!=|<=|>=|&&|\\|\\||"
                                + "\\+=|-=|\\*=|/=|%=|"
                                + "[+\\-*/%<>=!])"
                ).matcher(line);

        while (matcher.find()) {

            add(
                    events,
                    "OPERATOR",
                    matcher.group(),
                    lineNumber
            );
        }
    }

    // ============================================================
    // ADD EVENT
    // ============================================================

    private static void add(
            List<VisualizationEvent> events,
            String type,
            String value,
            int lineNumber) {

        events.add(
                new VisualizationEvent(
                        type,
                        value,
                        lineNumber
                )
        );
    }

    // ============================================================
    // FORMAT
    // ============================================================

    private static String format(
            double value) {

        if (value == Math.rint(value)) {

            return Long.toString(
                    (long) value
            );
        }

        return Double.toString(value);
    }
}
