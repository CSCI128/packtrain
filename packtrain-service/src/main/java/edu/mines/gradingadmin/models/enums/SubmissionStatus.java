package edu.mines.gradingadmin.models.enums;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.stream.Stream;

public enum SubmissionStatus {
    @JsonProperty("missing")
    MISSING("missing"),
    @JsonProperty("excused")
    EXCUSED("excused"),
    @JsonProperty("late")
    LATE("late"),
    @JsonProperty("extended")
    EXTENDED("extended"),
    @JsonProperty("on_time")
    ON_TIME("on_time");

    @Getter
    private final String status;

    SubmissionStatus(String status) {
        this.status = status;
    }

    public static SubmissionStatus fromString(String status) {
        return Stream.of(SubmissionStatus.values()).filter(t -> t.status.equals(status)).findFirst().orElseThrow(RuntimeException::new);
    }

}