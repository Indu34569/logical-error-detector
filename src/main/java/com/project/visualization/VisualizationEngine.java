package com.project.visualization;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VisualizationEngine {

    public static List<VisualizationEvent> createEvents(String code) {

        List<VisualizationEvent> events = new ArrayList<>();

        String[] lines = code.split("\\R");

        for (int i = 0; i < lines.length; i++) {

            String original = lines[i];
            String line = original.trim();

            if (line.isEmpty()) {
                continue;
            }

            int lineNumber = i + 1;

            // SOURCE: only the actual source line
            events.add(
                    new VisualizationEvent(
                            "SOURCE",
                            original,
                            lineNumber
                    )
            );

            // VARIABLE
            Matcher variableMatcher = Pattern.compile(
                    "(?:int|double|float|long|boolean|String)\\s+(\\w+)\\s*(?:=\\s*(.*))?;"
            ).matcher(line);

            if (variableMatcher.find()) {

                String variable = variableMatcher.group(1);
                String value = variableMatcher.group(2);

                if (value == null) {
                    value = "uninitialized";
                }

                events.add(
                        new VisualizationEvent(
                                "VARIABLE",
                                variable + " = " + value,
                                lineNumber
                        )
                );
            }

            // ASSIGNMENT / UPDATE
            Matcher assignmentMatcher = Pattern.compile(
                    "(\\w+)\\s*(\\+\\+|--|[+\\-*/]?=)\\s*(.*?);?$"
            ).matcher(line);

            if (assignmentMatcher.matches()
                    && !line.startsWith("if")
                    && !line.startsWith("while")
                    && !line.startsWith("for")) {

                String variable = assignmentMatcher.group(1);
                String operator = assignmentMatcher.group(2);
                String value = assignmentMatcher.group(3);

                events.add(
                        new VisualizationEvent(
                                "OPERATOR",
                                variable + " " + operator +
                                        (value.isEmpty() ? "" : " " + value),
                                lineNumber
                        )
                );
            }

            // CONDITIONS
            if (line.startsWith("if")
                    || line.startsWith("else if")
                    || line.startsWith("while")
                    || line.startsWith("for")) {

                String condition = line;

                events.add(
                        new VisualizationEvent(
                                "CONDITION",
                                condition,
                                lineNumber
                        )
                );
            }

            // LOOP
            if (line.startsWith("while")
                    || line.startsWith("for")) {

                events.add(
                        new VisualizationEvent(
                                "LOOP",
                                line,
                                lineNumber
                        )
                );
            }
        }

        return events;
    }
}
