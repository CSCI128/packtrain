package edu.mines.gradingadmin.models.enums;

public enum LateRequestStatus {
    IGNORED("ignored"),
    APPROVED("approved"),
    REJECTED("rejected"),
    PENDING("pending"),
    NO_EXTENSION("no_extension"),
    APPLIED("applied");

    private final String status;

    LateRequestStatus(String status){
        this.status = status;
    }
}
