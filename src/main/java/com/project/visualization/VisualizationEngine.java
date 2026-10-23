package com.project.visualization;

import java.util.ArrayList;
import java.util.List;

public class VisualizationEngine {

    public static List<VisualizationEvent> createEvents(String code) {
        List<VisualizationEvent> events = new ArrayList<>();

        String[] lines = code.split("\\R");

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.isEmpty()) {
                continue;
            }

            events.add(new VisualizationEvent("SOURCE", trimmed));

            if (trimmed.matches(".*\\b(int|double|float|long|boolean|String)\\s+\\w+\\s*=.*")) {
                events.add(new VisualizationEvent("VARIABLE", trimmed));
            }

            if (trimmed.contains("while") || trimmed.contains("for")) {
                events.add(new VisualizationEvent("LOOP", trimmed));
            }

            if (trimmed.startsWith("if") || trimmed.startsWith("else if")) {
                events.add(new VisualizationEvent("CONDITION", trimmed));
            }
        }

        return events;
    }
}
