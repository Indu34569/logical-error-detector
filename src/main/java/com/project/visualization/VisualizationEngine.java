
package com.project.visualization;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VisualizationEngine {

    public static List<VisualizationEvent> createEvents(String code) {

        List<VisualizationEvent> events = new ArrayList<>();

        String[] lines = code.split("\\R");

        for (String line : lines) {

            String trimmed = line.trim();

            if (trimmed.isEmpty()) {
                continue;
            }

            // SOURCE
            events.add(
                    new VisualizationEvent(
                            "SOURCE",
                            trimmed
                    )
            );

            // VARIABLE
            if (trimmed.matches(
                    ".*\\b(int|double|float|long|boolean|String|char)\\s+\\w+.*"
            )) {

                events.add(
                        new VisualizationEvent(
                                "VARIABLE",
                                trimmed
                        )
                );
            }

            // OPERATOR
            extractOperators(trimmed, events);

            // LOOP
            if (trimmed.matches(
                    ".*\\b(while|for|do)\\b.*"
            )) {

                events.add(
                        new VisualizationEvent(
                                "LOOP",
                                trimmed
                        )
                );
            }

            // CONDITION
            if (trimmed.matches(
                    "\\s*(if|else\\s+if)\\s*\\(.*"
            )) {

                events.add(
                        new VisualizationEvent(
                                "CONDITION",
                                trimmed
                        )
                );
            }
        }

        return events;
    }

    private static void extractOperators(
            String line,
            List<VisualizationEvent> events) {

        String operatorPattern =
                "(\\+\\+|--|==|!=|<=|>=|&&|\\|\\||\\+=|-=|\\*=|/=|%=|[+\\-*/%<>=!])";

        Pattern pattern = Pattern.compile(operatorPattern);

        Matcher matcher = pattern.matcher(line);

        while (matcher.find()) {

            String operator = matcher.group();

            // Avoid treating the declaration "=" as the only
            // interesting operator when there is no actual operation.
            events.add(
                    new VisualizationEvent(
                            "OPERATOR",
                            operator
                    )
            );
        }
    }
}
