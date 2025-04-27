package edu.mines.gradingadmin.models.enums;


import lombok.Getter;

import java.util.stream.Stream;

public enum SubmissionStatus {
    MISSING("missing"), EXCUSED("excused"), LATE("late"), EXTENDED("extended"), ON_TIME("on_time");

    @Getter
    private final String status;

    SubmissionStatus(String status) {
        this.status = status;
    }

    public static SubmissionStatus fromString(String status) {
        return Stream.of(SubmissionStatus.values()).filter(t -> t.status.equals(status)).findFirst().orElseThrow(RuntimeException::new);
    }

}