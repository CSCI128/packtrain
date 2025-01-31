package edu.mines.gradingadmin.models;

public enum ExternalSourceType {
    CANVAS("canvas");

    private final String type;

    ExternalSourceType(String type) {
        this.type = type;
    }
}
