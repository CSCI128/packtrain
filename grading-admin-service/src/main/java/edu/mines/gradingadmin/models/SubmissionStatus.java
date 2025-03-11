package edu.mines.gradingadmin.models;


import lombok.Getter;

public enum SubmissionStatus {
    MISSING("missing"), EXCUSED("excused"), LATE("late"), EXTENDED("extended"), ON_TIME("on_time");

    @Getter
    private final String status;

    SubmissionStatus(String status){
        this.status = status;
    }


}
