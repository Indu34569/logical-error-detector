package com.project.visualization;

public class VisualizationEvent {

    private final String type;
    private final String value;
    private final int lineNumber;

    public VisualizationEvent(String type, String value, int lineNumber) {
        this.type = type;
        this.value = value;
        this.lineNumber = lineNumber;
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
