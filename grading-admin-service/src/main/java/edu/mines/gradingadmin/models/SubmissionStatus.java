package edu.mines.gradingadmin.models;

import java.util.stream.Stream;

public enum SubmissionStatus {
    ONTIME("ontime"),
    LATE("late"),
    MISSING("missing"),
    UNKNOWN("unkown");

    private final String type;

    SubmissionStatus(String type) {this.type = type;}

    public static SubmissionStatus fromString(String type){
        return Stream.of(SubmissionStatus.values()).filter(t -> t.type.equals(type)).findFirst().orElseThrow(RuntimeException::new);
    }

}
