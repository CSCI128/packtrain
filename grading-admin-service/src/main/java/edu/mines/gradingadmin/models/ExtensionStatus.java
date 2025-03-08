package edu.mines.gradingadmin.models;

public enum ExtensionStatus {
    APPROVED("approved"),
    DENIED("denied"),
    PENDING("pending");

    private final String status;

    ExtensionStatus(String status){
        this.status = status;
    }
}
