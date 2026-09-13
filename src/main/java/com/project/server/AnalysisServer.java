package com.project.server;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
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

    private static final Path INPUT_FILE =
            Path.of("test-input/tests/WebInput.java");

    private static final Path DETECTOR_FILE =
            Path.of("test-input/tests/TestUnreachableWhile.java");


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


            // ==================================================
            // CORS PREFLIGHT
            // ==================================================

            if ("OPTIONS".equalsIgnoreCase(
                    exchange.getRequestMethod())) {

                exchange.sendResponseHeaders(
                        204,
                        -1
                );

                exchange.close();

                return;
            }


            // ==================================================
            // ONLY POST IS ALLOWED
            // ==================================================

            if (!"POST".equalsIgnoreCase(
                    exchange.getRequestMethod())) {

                sendResponse(
                        exchange,
                        405,
                        "Only POST requests are allowed."
                );

                return;
            }


            // ==================================================
            // READ FRONTEND CODE
            // ==================================================

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


            // ==================================================
            // SAVE ORIGINAL CODE
            // ==================================================

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


            // ==================================================
            // RUN COMBINED ANALYSIS
            // ==================================================

            String result =
                    analyzeCode(code);


            // ==================================================
            // SEND RESULT TO FRONTEND
            // ==================================================

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


    // ==========================================================
    // COMBINED SYNTAX + LOGICAL ANALYSIS
    // ==========================================================

    private static String analyzeCode(
            String originalCode)
            throws Exception {

        StringBuilder finalReport =
                new StringBuilder();


        // ==================================================
        // STEP 1: SYNTAX ANALYSIS
        // ==================================================

        boolean syntaxError = false;

        String syntaxMessage = "";


        try {

            StaticJavaParser.parse(
                    originalCode
            );

        } catch (Exception e) {

            syntaxError = true;

            syntaxMessage =
                    e.getMessage() == null
                            ? "Java syntax error detected."
                            : e.getMessage();
        }


        // ==================================================
        // STEP 2: PRINT SYNTAX RESULT
        // ==================================================

        finalReport.append(
                "SYNTAX ANALYSIS\n"
        );

        finalReport.append(
                "----------------------------------------\n"
        );


        if (!syntaxError) {

            finalReport.append(
                    "No syntax errors found\n"
            );

            finalReport.append("\n");

        } else {

            finalReport.append(
                    "Syntax errors detected!\n"
            );

            finalReport.append("\n");

            finalReport.append(
                    "Syntax Error Details:\n"
            );

            finalReport.append(
                    "----------------------------------------\n"
            );

            finalReport.append(
                    formatSyntaxError(
                            syntaxMessage,
                            originalCode
                    )
            );

            finalReport.append("\n\n");
        }


        // ==================================================
        // STEP 3: LOGICAL ANALYSIS
        // ==================================================

        /*
         * If the original program is syntactically valid,
         * analyze it directly.
         *
         * If syntax errors exist, attempt a safe recovery
         * for common errors such as a missing semicolon.
         *
         * This allows the logical analyzer to continue when
         * the rest of the program is still understandable.
         */

        String codeForLogicalAnalysis =
                originalCode;


        if (syntaxError) {

            codeForLogicalAnalysis =
                    repairCommonSyntaxErrors(
                            originalCode
                    );
        }


        // ==================================================
        // STEP 4: CHECK WHETHER RECOVERED CODE PARSES
        // ==================================================

        boolean recoveredSuccessfully = false;


        try {

            StaticJavaParser.parse(
                    codeForLogicalAnalysis
            );

            recoveredSuccessfully = true;

        } catch (Exception ignored) {

            recoveredSuccessfully = false;
        }


        // ==================================================
        // STEP 5: RUN EXISTING LOGICAL DETECTOR
        // ==================================================

        finalReport.append(
                "LOGICAL ERROR ANALYSIS\n"
        );

        finalReport.append(
                "----------------------------------------\n"
        );


        if (recoveredSuccessfully) {

            Files.createDirectories(
                    DETECTOR_FILE.getParent()
            );

            Files.writeString(
                    DETECTOR_FILE,
                    codeForLogicalAnalysis,
                    StandardCharsets.UTF_8
            );


            String logicalResult =
                    runLogicalDetector();


            finalReport.append(
                    logicalResult
            );

        } else {

            finalReport.append(
                    "Logical analysis could not be completed "
                            + "because the syntax errors could not "
                            + "be safely recovered.\n"
            );
        }


        return finalReport.toString();
    }


    // ==========================================================
    // RUN EXISTING LOGICAL ERROR DETECTOR
    // ==========================================================

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

            System.setOut(capture);


            LogicalErrorDetector.main(
                    new String[0]
            );


        } finally {

            System.setOut(originalOut);

            capture.close();
        }


        return output.toString(
                StandardCharsets.UTF_8
        );
    }


    // ==========================================================
    // COMMON SYNTAX ERROR RECOVERY
    // ==========================================================

    private static String repairCommonSyntaxErrors(
            String code) {

        String[] lines =
                code.split(
                        "\\R",
                        -1
                );


        List<String> repaired =
                new ArrayList<>();


        for (int i = 0; i < lines.length; i++) {

            String line =
                    lines[i];


            String trimmed =
                    line.trim();


            // --------------------------------------------------
            // Keep empty lines unchanged
            // --------------------------------------------------

            if (trimmed.isEmpty()) {

                repaired.add(line);

                continue;
            }


            // --------------------------------------------------
            // Do not modify structural lines
            // --------------------------------------------------

            if (trimmed.startsWith("//")
                    || trimmed.startsWith("/*")
                    || trimmed.startsWith("*")
                    || trimmed.startsWith("*/")
                    || trimmed.equals("{")
                    || trimmed.equals("}")
                    || trimmed.endsWith("{")
                    || trimmed.endsWith("}")
                    || trimmed.endsWith(";")) {

                repaired.add(line);

                continue;
            }


            // --------------------------------------------------
            // Do not modify control statements
            // --------------------------------------------------

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
                    || trimmed.startsWith("catch ")
                    || trimmed.startsWith("catch(")
                    || trimmed.startsWith("catch (")
                    || trimmed.startsWith("else")
                    || trimmed.startsWith("try")
                    || trimmed.startsWith("finally")) {

                repaired.add(line);

                continue;
            }


            // --------------------------------------------------
            // Add missing semicolon to simple declarations
            // --------------------------------------------------

            if (looksLikeVariableDeclaration(trimmed)) {

                repaired.add(
                        line + ";"
                );

                continue;
            }


            // --------------------------------------------------
            // Add missing semicolon to simple assignments
            // --------------------------------------------------

            if (looksLikeSimpleAssignment(trimmed)) {

                repaired.add(
                        line + ";"
                );

                continue;
            }


            // --------------------------------------------------
            // Add missing semicolon to return statements
            // --------------------------------------------------

            if (trimmed.startsWith("return ")
                    && !trimmed.endsWith(";")) {

                repaired.add(
                        line + ";"
                );

                continue;
            }


            // --------------------------------------------------
            // Add missing semicolon to break/continue
            // --------------------------------------------------

            if (trimmed.equals("break")
                    || trimmed.equals("continue")) {

                repaired.add(
                        line + ";"
                );

                continue;
            }


            // --------------------------------------------------
            // Otherwise leave line unchanged
            // --------------------------------------------------

            repaired.add(line);
        }


        return String.join(
                System.lineSeparator(),
                repaired
        );
    }


    // ==========================================================
    // CHECK VARIABLE DECLARATION
    // ==========================================================

    private static boolean looksLikeVariableDeclaration(
            String line) {

        return line.matches(
                "^(final\\s+)?"
                        + "(byte|short|int|long|float|double|char|boolean|String)"
                        + "\\s+[A-Za-z_$][A-Za-z0-9_$]*"
                        + "(\\s*=.*)?$"
        );
    }


    // ==========================================================
    // CHECK SIMPLE ASSIGNMENT
    // ==========================================================

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
                "^[A-Za-z_$][A-Za-z0-9_$]*\\s*="
                        + "\\s*[^;]+$"
        );
    }


    // ==========================================================
    // FORMAT SYNTAX ERROR
    // ==========================================================

    private static String formatSyntaxError(
            String message,
            String code) {

        StringBuilder result =
                new StringBuilder();


        int lineNumber =
                findSyntaxErrorLine(
                        message
                );


        if (lineNumber > 0) {

            result.append(
                    "Line "
                            + lineNumber
                            + ": "
            );

        } else {

            result.append(
                    "Syntax Error: "
            );
        }


        result.append(
                cleanSyntaxMessage(message)
        );


        return result.toString();
    }


    // ==========================================================
    // FIND LINE NUMBER FROM JAVAPARSER MESSAGE
    // ==========================================================

    private static int findSyntaxErrorLine(
            String message) {

        if (message == null) {
            return -1;
        }


        /*
         * JavaParser messages commonly contain:
         *
         * line 7, column 18
         *
         * or:
         *
         * (line 7,col 18)
         */

        java.util.regex.Pattern pattern =
                java.util.regex.Pattern.compile(
                        "(?:line\\s+|line\\s*\\()"
                                + "(\\d+)",
                        java.util.regex.Pattern.CASE_INSENSITIVE
                );


        java.util.regex.Matcher matcher =
                pattern.matcher(message);


        if (matcher.find()) {

            try {

                return Integer.parseInt(
                        matcher.group(1)
                );

            } catch (NumberFormatException ignored) {
            }
        }


        return -1;
    }


    // ==========================================================
    // CLEAN SYNTAX MESSAGE
    // ==========================================================

    private static String cleanSyntaxMessage(
            String message) {

        if (message == null
                || message.trim().isEmpty()) {

            return "Java syntax error detected.";
        }


        String cleaned =
                message.replace(
                        "\n",
                        " "
                ).trim();


        if (cleaned.length() > 500) {

            cleaned =
                    cleaned.substring(
                            0,
                            500
                    );
        }


        return cleaned;
    }


    // ==========================================================
    // READ REQUEST BODY
    // ==========================================================

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


    // ==========================================================
    // CORS HEADERS
    // ==========================================================

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


    // ==========================================================
    // SEND RESPONSE
    // ==========================================================

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