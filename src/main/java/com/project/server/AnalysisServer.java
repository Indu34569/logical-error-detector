package com.project.server;

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
import java.util.concurrent.Executors;

public class AnalysisServer {

    private static final int PORT =
        Integer.parseInt(
                System.getenv().getOrDefault("PORT", "8080")
        );

    private static final Path INPUT_FILE =
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
                "Server running on port:" + PORT
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

            if ("OPTIONS".equalsIgnoreCase(
                    exchange.getRequestMethod())) {

                exchange.sendResponseHeaders(
                        204,
                        -1
                );

                exchange.close();

                return;
            }

            if (!"POST".equalsIgnoreCase(
                    exchange.getRequestMethod())) {

                sendResponse(
                        exchange,
                        405,
                        "Only POST requests are allowed."
                );

                return;
            }

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

            String result =
                    runAnalyzer();

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

    private static String runAnalyzer()
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

            /*
             * The current detector reads:
             *
             * test-input/tests/TestUnreachableWhile.java
             *
             * Therefore we temporarily copy the user's
             * frontend input into that exact file.
             */

            Path detectorFile =
                    Path.of(
                            "test-input/tests/TestUnreachableWhile.java"
                    );

            Files.createDirectories(
                    detectorFile.getParent()
            );

            Files.copy(
                    INPUT_FILE,
                    detectorFile,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );

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