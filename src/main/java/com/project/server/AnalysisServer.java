package com.project.server;

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
import java.util.List;
import java.util.concurrent.Executors;

public class AnalysisServer {

    private static final int PORT =
            Integer.parseInt(
                    System.getenv().getOrDefault("PORT", "8080")
            );

    /*
     * The frontend sends code here.
     */
    private static final Path INPUT_FILE =
            Path.of("test-input/tests/WebInput.java");

    /*
     * IMPORTANT:
     * The LogicalErrorDetector replacement reads WebInput.java.
     * Therefore the detector must receive the user's code here too.
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
             * CORS preflight
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
             * Only POST is allowed.
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
             * Read Java code sent by frontend.
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
             * Perform BOTH syntax and logical analysis.
             */
            String result =
                    analyzeCode(code);


            /*
             * Always return the complete report.
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


        /*
         * Use JavaParser directly so that we can collect
         * syntax problems without stopping the whole analysis.
         */
        try {

            ParseResult<com.github.javaparser.ast.CompilationUnit>
                    parseResult =
                    new com.github.javaparser.JavaParser()
                            .parse(originalCode);


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
                    formatException(
                            e
                    )
            );

            report.append("\n\n");
        }


        /*
         * ======================================================
         * STEP 2: RECOVER CODE FOR LOGICAL ANALYSIS
         * ======================================================
         *
         * If there is a common syntax error such as:
         *
         * int salary = 30000
         *
         * we repair it to:
         *
         * int salary = 30000;
         *
         * while preserving the rest of the user's code.
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


        /*
         * IMPORTANT:
         *
         * Even when syntax errors exist, we still attempt
         * logical analysis.
         */
        if (recoveredSuccessfully) {

            Files.createDirectories(
                    DETECTOR_FILE.getParent()
            );


            /*
             * Write recovered code to the SAME file
             * LogicalErrorDetector reads.
             */
            Files.writeString(
                    DETECTOR_FILE,
                    logicalCode,
                    StandardCharsets.UTF_8
            );


            String logicalResult =
                    runLogicalDetector();


            /*
             * Remove the detector's own header if present.
             * The frontend only needs the analysis content.
             */
            report.append(
                    cleanLogicalResult(
                            logicalResult
                    )
            );

        } else {

            /*
             * Even if full recovery fails, perform a
             * lightweight logical analysis directly here.
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
     * LIGHTWEIGHT LOGICAL ANALYSIS
     * ==========================================================
     *
     * This is a safety fallback.
     *
     * Example:
     *
     * int age = 20;
     *
     * if (age < 10) {
     *
     * }
     *
     * It detects that age < 10 is impossible.
     */
    private static String lightweightLogicalAnalysis(
            String code) {

        StringBuilder result =
                new StringBuilder();

        java.util.Map<String, Integer> variables =
                new java.util.LinkedHashMap<>();


        String[] lines =
                code.split(
                        "\\R",
                        -1
                );


        /*
         * First collect integer variables.
         */
        for (String originalLine : lines) {

            String line =
                    removeComment(
                            originalLine
                    ).trim();


            java.util.regex.Matcher declaration =
                    java.util.regex.Pattern.compile(
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


            java.util.regex.Matcher assignment =
                    java.util.regex.Pattern.compile(
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
         * Then check conditions.
         */
        for (int i = 0; i < lines.length; i++) {

            String line =
                    removeComment(
                            lines[i]
                    ).trim();


            java.util.regex.Matcher conditionMatcher =
                    java.util.regex.Pattern.compile(
                            "\\b(?:if|while|for)\\s*\\(([^)]*)\\)"
                    ).matcher(line);


            if (!conditionMatcher.find()) {
                continue;
            }


            String condition =
                    conditionMatcher.group(1).trim();


            java.util.regex.Matcher comparison =
                    java.util.regex.Pattern.compile(
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
             * Control statements should not receive
             * an automatic semicolon.
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
        	   location.getBegin().getRange().get().begin.line;

	    int column =
        	   location.getBegin().getRange().get().begin.column;


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
     * CLEAN LOGICAL RESULT
     * ==========================================================
     */
    private static String cleanLogicalResult(
            String result) {

        if (result == null
                || result.trim().isEmpty()) {

            return "No logical errors detected.\n";
        }


        return result;
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