package com.project.visualization;

public class VisualizationEvent {
    private final String type;
    private final String value;

    public VisualizationEvent(String type, String value) {
        this.type = type;
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }
}
