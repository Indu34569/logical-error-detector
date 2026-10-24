package com.project.server;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.Problem;
import com.github.javaparser.StaticJavaParser;
import com.project.analyzer.LogicalErrorDetector;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnalysisServer {

    private static final int PORT =
            Integer.parseInt(
                    System.getenv().getOrDefault("PORT", "8080")
            );

    /*
     * Java code received from the frontend is saved here.
     */
    private static final Path INPUT_FILE =
            Path.of("test-input/tests/WebInput.java");

    /*
     * LogicalErrorDetector reads this same file.
     */
    private static final Path DETECTOR_FILE =
            Path.of("test-input/tests/WebInput.java");


    public static void main(String[] args) throws Exception {

        HttpServer server =
                HttpServer.create(
                        new InetSocketAddress("0.0.0.0", PORT),
                        0
                );

        server.createContext(
                "/analyze",
                AnalysisServer::handleAnalyze
        );

        server.setExecutor(
                Executors.newFixedThreadPool(4)
        );

        server.start();

        System.out.println("========================================");
        System.out.println("       LOGICAL ERROR ANALYSIS SERVER");
        System.out.println("========================================");
        System.out.println();

        System.out.println(
                "Server running on port: " + PORT
        );

        System.out.println();

        System.out.println(
                "Waiting for Java code from frontend..."
        );

        System.out.println();
    }


    private static void handleAnalyze(
            HttpExchange exchange) {

        try {

            addCorsHeaders(exchange);

            /*
             * CORS preflight.
             */
            if ("OPTIONS".equalsIgnoreCase(
                    exchange.getRequestMethod())) {

                exchange.sendResponseHeaders(
                        204,
                        -1
                );

                exchange.close();

                return;
            }

            /*
             * Only POST requests are accepted.
             */
            if (!"POST".equalsIgnoreCase(
                    exchange.getRequestMethod())) {

                sendResponse(
                        exchange,
                        405,
                        "Only POST requests are allowed."
                );

                return;
            }

            /*
             * Read code sent by frontend.
             */
            String code =
                    readRequestBody(
                            exchange.getRequestBody()
                    );

            if (code.trim().isEmpty()) {

                sendResponse(
                        exchange,
                        400,
                        "Please provide Java code."
                );

                return;
            }

            /*
             * Save exactly what the user entered.
             */
            Files.createDirectories(
                    INPUT_FILE.getParent()
            );

            Files.writeString(
                    INPUT_FILE,
                    code,
                    StandardCharsets.UTF_8
            );

            System.out.println(
                    "Received Java program from frontend."
            );

            System.out.println(
                    "Analyzing..."
            );

            /*
             * Perform syntax + logical analysis.
             */
            String result =
                    analyzeCode(code);

            /*
             * Return one clean report.
             */
            sendResponse(
                    exchange,
                    200,
                    result
            );

            System.out.println(
                    "Analysis completed."
            );

            System.out.println();

        } catch (Exception e) {

            e.printStackTrace();

            try {

                sendResponse(
                        exchange,
                        500,
                        "Backend analysis error:\n"
                                + e.getMessage()
                );

            } catch (Exception ignored) {
            }

        } finally {

            exchange.close();
        }
    }


    /*
     * ==========================================================
     * COMBINED SYNTAX + LOGICAL ANALYSIS
     * ==========================================================
     */
    private static String analyzeCode(
            String originalCode)
            throws Exception {

        StringBuilder report =
                new StringBuilder();

        /*
         * ======================================================
         * STEP 1: SYNTAX ANALYSIS
         * ======================================================
         */

        report.append(
                "========================================\n"
        );

        report.append(
                "       STATIC ANALYSIS TOOL\n"
        );

        report.append(
                "========================================\n\n"
        );

        report.append(
                "File: "
                        + INPUT_FILE
                        + "\n\n"
        );

        report.append(
                "SYNTAX ANALYSIS\n"
        );

        report.append(
                "----------------------------------------\n"
        );

        boolean syntaxError = false;

        List<Problem> syntaxProblems =
                new ArrayList<>();

        try {

            JavaParser parser =
                    new JavaParser();

            ParseResult<com.github.javaparser.ast.CompilationUnit>
                    parseResult =
                    parser.parse(originalCode);

            if (parseResult.getProblems().isEmpty()) {

                report.append(
                        "No syntax errors found.\n\n"
                );

            } else {

                syntaxError = true;

                syntaxProblems.addAll(
                        parseResult.getProblems()
                );

                report.append(
                        "Syntax errors detected!\n\n"
                );

                report.append(
                        "Syntax Error Details:\n"
                );

                report.append(
                        "----------------------------------------\n"
                );

                for (Problem problem : syntaxProblems) {

                    report.append(
                            formatProblem(problem)
                    );

                    report.append("\n");
                }

                report.append("\n");
            }

        } catch (Exception e) {

            syntaxError = true;

            report.append(
                    "Syntax errors detected!\n\n"
            );

            report.append(
                    "Syntax Error Details:\n"
            );

            report.append(
                    "----------------------------------------\n"
            );

            report.append(
                    formatException(e)
            );

            report.append("\n\n");
        }


        /*
         * ======================================================
         * STEP 2: RECOVER COMMON SYNTAX ERRORS
         * ======================================================
         */

        String logicalCode =
                repairCommonSyntaxErrors(
                        originalCode
                );


        /*
         * ======================================================
         * STEP 3: CHECK RECOVERED CODE
         * ======================================================
         */

        boolean recoveredSuccessfully =
                false;

        try {

            StaticJavaParser.parse(
                    logicalCode
            );

            recoveredSuccessfully = true;

        } catch (Exception ignored) {

            recoveredSuccessfully = false;
        }


        /*
         * ======================================================
         * STEP 4: LOGICAL ANALYSIS
         * ======================================================
         */

        report.append(
                "LOGICAL ERROR ANALYSIS\n"
        );

        report.append(
                "----------------------------------------\n"
        );


        if (recoveredSuccessfully) {

            Files.createDirectories(
                    DETECTOR_FILE.getParent()
            );

            /*
             * The detector reads WebInput.java.
             */
            Files.writeString(
                    DETECTOR_FILE,
                    logicalCode,
                    StandardCharsets.UTF_8
            );

            String logicalResult =
                    runLogicalDetector();

            /*
             * Clean the detector output before
             * adding it to the server report.
             */
            report.append(
                    cleanLogicalResult(
                            logicalResult
                    )
            );

        } else {

            /*
             * If complete recovery is impossible,
             * use lightweight logical analysis.
             */
            String lightweightResult =
                    lightweightLogicalAnalysis(
                            logicalCode
                    );

            report.append(
                    lightweightResult
            );
        }


        /*
         * ======================================================
         * STEP 5: FINAL SUMMARY
         * ======================================================
         */

        report.append("\n\n");

        report.append(
                "========================================\n"
        );

        if (syntaxError) {

            report.append(
                    "Syntax errors were detected.\n"
            );

            report.append(
                    "Logical analysis was also performed.\n"
            );

        } else {

            report.append(
                    "No syntax errors were detected.\n"
            );

            report.append(
                    "Logical analysis was completed.\n"
            );
        }

        report.append(
                "========================================\n"
        );

        return report.toString();
    }


    /*
     * ==========================================================
     * RUN LOGICAL ERROR DETECTOR
     * ==========================================================
     */
    private static String runLogicalDetector()
            throws Exception {

        PrintStream originalOut =
                System.out;

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        PrintStream capture =
                new PrintStream(
                        output,
                        true,
                        StandardCharsets.UTF_8
                );

        try {

            System.setOut(
                    capture
            );

            LogicalErrorDetector.main(
                    new String[0]
            );

        } finally {

            System.setOut(
                    originalOut
            );

            capture.close();
        }

        return output.toString(
                StandardCharsets.UTF_8
        );
    }


    /*
     * ==========================================================
     * CLEAN LOGICAL DETECTOR OUTPUT
     * ==========================================================
     *
     * IMPORTANT:
     *
     * The LogicalErrorDetector prints its own report.
     * The server must extract ONLY the useful logical
     * analysis section.
     *
     * This prevents:
     *
     * 1. Duplicate STATIC ANALYSIS TOOL headings
     * 2. Duplicate SYNTAX ANALYSIS sections
     * 3. Duplicate FINAL ANALYSIS REPORT sections
     * 4. Duplicate summary messages
     * 5. Stray IF / ELSE / LOOP lines appearing
     *    inside the frontend error description
     */
    private static String cleanLogicalResult(
            String result) {

        if (result == null
                || result.trim().isEmpty()) {

            return "No logical errors detected.\n";
        }

        String cleaned =
                result.trim();


        /*
         * ------------------------------------------------------
         * Remove everything BEFORE LOGICAL ANALYSIS.
         * ------------------------------------------------------
         *
         * The detector normally prints:
         *
         * STATIC ANALYSIS TOOL
         * File
         * SYNTAX ANALYSIS
         * ...
         * LOGICAL ANALYSIS
         *
         * We only want the part beginning with
         * LOGICAL ANALYSIS.
         */
        int logicalIndex =
                cleaned.indexOf(
                        "LOGICAL ANALYSIS"
                );

        if (logicalIndex >= 0) {

            cleaned =
                    cleaned.substring(
                            logicalIndex
                    );

        } else {

            /*
             * Compatibility with detector versions
             * that may use LOGICAL ERROR ANALYSIS.
             */
            int logicalErrorIndex =
                    cleaned.indexOf(
                            "LOGICAL ERROR ANALYSIS"
                    );

            if (logicalErrorIndex >= 0) {

                cleaned =
                        cleaned.substring(
                                logicalErrorIndex
                        );
            }
        }


        /*
         * ------------------------------------------------------
         * Remove everything AFTER FINAL ANALYSIS REPORT.
         * ------------------------------------------------------
         */
        int finalReportIndex =
                cleaned.indexOf(
                        "FINAL ANALYSIS REPORT"
                );

        if (finalReportIndex >= 0) {

            cleaned =
                    cleaned.substring(
                            0,
                            finalReportIndex
                    );
        }


        /*
         * ------------------------------------------------------
         * Remove duplicate outer headings if present.
         * ------------------------------------------------------
         */
        cleaned =
                cleaned.replaceAll(
                        "(?s)^\\s*=+\\s*",
                        ""
                );


        /*
         * ------------------------------------------------------
         * Remove duplicate summary sentences.
         * ------------------------------------------------------
         */
        cleaned =
                cleaned.replace(
                        "Only logical errors were detected.",
                        ""
                );

        cleaned =
                cleaned.replace(
                        "No logical errors detected.",
                        ""
                );

        cleaned =
                cleaned.replace(
                        "No syntax errors were detected.",
                        ""
                );

        cleaned =
                cleaned.replace(
                        "Syntax errors were detected.",
                        ""
                );


        /*
         * ------------------------------------------------------
         * Remove accidental duplicate logical-analysis
         * headings that may occur in older detector output.
         * ------------------------------------------------------
         */
        cleaned =
                cleaned.replace(
                        "LOGICAL ERROR ANALYSIS\n"
                                + "----------------------------------------\n"
                                + "LOGICAL ANALYSIS\n"
                                + "----------------------------------------\n",
                        "LOGICAL ANALYSIS\n"
                                + "----------------------------------------\n"
                );


        /*
         * ------------------------------------------------------
         * Remove excessive blank lines.
         * ------------------------------------------------------
         */
        cleaned =
                cleaned.replaceAll(
                        "\\n{3,}",
                        "\n\n"
                );


        cleaned =
                cleaned.trim();


        /*
         * ------------------------------------------------------
         * Final fallback.
         * ------------------------------------------------------
         */
        if (cleaned.isEmpty()) {

            return "No logical errors detected.\n";
        }


        return cleaned + "\n";
    }


    /*
     * ==========================================================
     * LIGHTWEIGHT LOGICAL ANALYSIS
     * ==========================================================
     */
    private static String lightweightLogicalAnalysis(
            String code) {

        StringBuilder result =
                new StringBuilder();

        Map<String, Integer> variables =
                new LinkedHashMap<>();

        String[] lines =
                code.split(
                        "\\R",
                        -1
                );


        /*
         * First collect integer variables and assignments.
         */
        for (String originalLine : lines) {

            String line =
                    removeComment(
                            originalLine
                    ).trim();

            Matcher declaration =
                    Pattern.compile(
                            "\\b(?:int|long|short|byte)\\s+"
                                    + "([A-Za-z_$][A-Za-z0-9_$]*)"
                                    + "\\s*=\\s*(-?\\d+)"
                    ).matcher(line);

            if (declaration.find()) {

                variables.put(
                        declaration.group(1),
                        Integer.parseInt(
                                declaration.group(2)
                        )
                );
            }

            Matcher assignment =
                    Pattern.compile(
                            "^([A-Za-z_$][A-Za-z0-9_$]*)"
                                    + "\\s*=\\s*(-?\\d+)\\s*;?$"
                    ).matcher(line);

            if (assignment.find()) {

                variables.put(
                        assignment.group(1),
                        Integer.parseInt(
                                assignment.group(2)
                        )
                );
            }
        }


        /*
         * Check conditions.
         */
        for (int i = 0; i < lines.length; i++) {

            String line =
                    removeComment(
                            lines[i]
                    ).trim();

            Matcher conditionMatcher =
                    Pattern.compile(
                            "\\b(?:if|while|for)\\s*\\(([^)]*)\\)"
                    ).matcher(line);

            if (!conditionMatcher.find()) {

                continue;
            }

            String condition =
                    conditionMatcher.group(1).trim();

            Matcher comparison =
                    Pattern.compile(
                            "^([A-Za-z_$][A-Za-z0-9_$]*)"
                                    + "\\s*(<=|>=|==|!=|<|>)"
                                    + "\\s*(-?\\d+)$"
                    ).matcher(condition);

            if (!comparison.find()) {

                continue;
            }

            String variable =
                    comparison.group(1);

            String operator =
                    comparison.group(2);

            int right =
                    Integer.parseInt(
                            comparison.group(3)
                    );

            if (!variables.containsKey(variable)) {

                continue;
            }

            int left =
                    variables.get(variable);

            boolean conditionResult =
                    evaluate(
                            left,
                            operator,
                            right
                    );

            if (!conditionResult) {

                result.append(
                        "\nLogical Error Detected!\n"
                );

                result.append(
                        "----------------------------------------\n"
                );

                result.append(
                        "Error Type: "
                                + "Impossible Relational Condition\n"
                );

                result.append(
                        "Line: "
                                + (i + 1)
                                + "\n"
                );

                result.append(
                        "Condition: "
                                + condition
                                + "\n"
                );

                result.append(
                        "Condition '"
                                + condition
                                + "' is false for the known variable values.\n"
                );
            }
        }


        if (result.length() == 0) {

            result.append(
                    "No logical errors detected.\n"
            );
        }

        return result.toString();
    }


    /*
     * ==========================================================
     * REPAIR COMMON SYNTAX ERRORS
     * ==========================================================
     */
    private static String repairCommonSyntaxErrors(
            String code) {

        String[] lines =
                code.split(
                        "\\R",
                        -1
                );

        List<String> repaired =
                new ArrayList<>();

        for (String line : lines) {

            String trimmed =
                    line.trim();

            if (trimmed.isEmpty()) {

                repaired.add(line);

                continue;
            }

            /*
             * Comments and structural lines.
             */
            if (trimmed.startsWith("//")
                    || trimmed.startsWith("/*")
                    || trimmed.startsWith("*")
                    || trimmed.startsWith("*/")
                    || trimmed.equals("{")
                    || trimmed.equals("}")
                    || trimmed.endsWith("{")
                    || trimmed.endsWith("}")) {

                repaired.add(line);

                continue;
            }

            /*
             * Control statements must not receive
             * automatic semicolons.
             */
            if (trimmed.startsWith("if ")
                    || trimmed.startsWith("if(")
                    || trimmed.startsWith("if (")
                    || trimmed.startsWith("while ")
                    || trimmed.startsWith("while(")
                    || trimmed.startsWith("while (")
                    || trimmed.startsWith("for ")
                    || trimmed.startsWith("for(")
                    || trimmed.startsWith("for (")
                    || trimmed.startsWith("switch ")
                    || trimmed.startsWith("switch(")
                    || trimmed.startsWith("switch (")
                    || trimmed.startsWith("else")
                    || trimmed.startsWith("try")
                    || trimmed.startsWith("catch")
                    || trimmed.startsWith("finally")) {

                repaired.add(line);

                continue;
            }

            /*
             * Already has semicolon.
             */
            if (trimmed.endsWith(";")) {

                repaired.add(line);

                continue;
            }

            /*
             * Variable declaration.
             */
            if (looksLikeVariableDeclaration(trimmed)) {

                repaired.add(
                        line + ";"
                );

                continue;
            }

            /*
             * Simple assignment.
             */
            if (looksLikeSimpleAssignment(trimmed)) {

                repaired.add(
                        line + ";"
                );

                continue;
            }

            /*
             * return statement.
             */
            if (trimmed.startsWith("return ")) {

                repaired.add(
                        line + ";"
                );

                continue;
            }

            /*
             * break / continue.
             */
            if (trimmed.equals("break")
                    || trimmed.equals("continue")) {

                repaired.add(
                        line + ";"
                );

                continue;
            }

            repaired.add(line);
        }

        return String.join(
                System.lineSeparator(),
                repaired
        );
    }


    /*
     * ==========================================================
     * VARIABLE DECLARATION CHECK
     * ==========================================================
     */
    private static boolean looksLikeVariableDeclaration(
            String line) {

        return line.matches(
                "^(final\\s+)?"
                        + "(byte|short|int|long|float|double|char|boolean|String)"
                        + "\\s+[A-Za-z_$][A-Za-z0-9_$]*"
                        + "(\\s*=.*)?$"
        );
    }


    /*
     * ==========================================================
     * SIMPLE ASSIGNMENT CHECK
     * ==========================================================
     */
    private static boolean looksLikeSimpleAssignment(
            String line) {

        if (!line.contains("=")) {

            return false;
        }

        if (line.contains("==")
                || line.contains(">=")
                || line.contains("<=")
                || line.contains("!=")
                || line.contains("=>")
                || line.contains("=<")) {

            return false;
        }

        return line.matches(
                "^[A-Za-z_$][A-Za-z0-9_$]*\\s*=\\s*[^;]+$"
        );
    }


    /*
     * ==========================================================
     * INTEGER CONDITION EVALUATION
     * ==========================================================
     */
    private static boolean evaluate(
            int left,
            String operator,
            int right) {

        switch (operator) {

            case "<":
                return left < right;

            case ">":
                return left > right;

            case "<=":
                return left <= right;

            case ">=":
                return left >= right;

            case "==":
                return left == right;

            case "!=":
                return left != right;

            default:
                return true;
        }
    }


    /*
     * ==========================================================
     * REMOVE SINGLE-LINE COMMENTS
     * ==========================================================
     */
    private static String removeComment(
            String line) {

        int index =
                line.indexOf("//");

        if (index >= 0) {

            return line.substring(
                    0,
                    index
            );
        }

        return line;
    }


    /*
     * ==========================================================
     * FORMAT JAVAPARSER PROBLEM
     * ==========================================================
     */
    private static String formatProblem(
            Problem problem) {

        String message =
                problem.getMessage();

        StringBuilder result =
                new StringBuilder();

        if (problem.getLocation().isPresent()) {

            var location =
                    problem.getLocation().get();

            int line =
                    location
                            .getBegin()
                            .getRange()
                            .get()
                            .begin
                            .line;

            int column =
                    location
                            .getBegin()
                            .getRange()
                            .get()
                            .begin
                            .column;

            result.append(
                    "Line "
                            + line
                            + ", Column "
                            + column
                            + ": "
            );
        }

        result.append(
                message
        );

        result.append("\n");

        return result.toString();
    }


    /*
     * ==========================================================
     * FORMAT EXCEPTION
     * ==========================================================
     */
    private static String formatException(
            Exception exception) {

        String message =
                exception.getMessage();

        if (message == null
                || message.trim().isEmpty()) {

            return "Java syntax error detected.\n";
        }

        return message + "\n";
    }


    /*
     * ==========================================================
     * READ REQUEST BODY
     * ==========================================================
     */
    private static String readRequestBody(
            InputStream inputStream)
            throws IOException {

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        byte[] buffer =
                new byte[4096];

        int length;

        while ((length =
                inputStream.read(buffer)) != -1) {

            output.write(
                    buffer,
                    0,
                    length
            );
        }

        return output.toString(
                StandardCharsets.UTF_8
        );
    }


    /*
     * ==========================================================
     * CORS
     * ==========================================================
     */
    private static void addCorsHeaders(
            HttpExchange exchange) {

        Headers headers =
                exchange.getResponseHeaders();

        headers.set(
                "Access-Control-Allow-Origin",
                "*"
        );

        headers.set(
                "Access-Control-Allow-Methods",
                "POST, OPTIONS"
        );

        headers.set(
                "Access-Control-Allow-Headers",
                "Content-Type"
        );

        headers.set(
                "Content-Type",
                "text/plain; charset=UTF-8"
        );
    }


    /*
     * ==========================================================
     * SEND RESPONSE
     * ==========================================================
     */
    private static void sendResponse(
            HttpExchange exchange,
            int statusCode,
            String response)
            throws IOException {

        byte[] data =
                response.getBytes(
                        StandardCharsets.UTF_8
                );

        exchange.sendResponseHeaders(
                statusCode,
                data.length
        );

        exchange.getResponseBody().write(
                data
        );

        exchange.getResponseBody().close();
    }
}