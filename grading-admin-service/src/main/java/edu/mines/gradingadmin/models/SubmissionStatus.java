package edu.mines.gradingadmin.models;

import lombok.Getter;

import java.util.stream.Stream;

public enum SubmissionStatus {
    GRADED("Graded"),
    UNGRADED("Ungraded"),
    MISSING("Missing"),
    UNKNOWN("Unknown");

    private final String type;

    SubmissionStatus(String type) {this.type = type;}

    public static SubmissionStatus fromString(String type){
        return Stream.of(SubmissionStatus.values()).filter(t -> t.type.equals(type)).findFirst().orElseThrow(RuntimeException::new);
    }

}
