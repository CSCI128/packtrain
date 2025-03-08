package edu.mines.gradingadmin.models;

public enum RequestStatus {
    APPROVED("approved"),
    DENIED("denied"),
    PENDING("pending");

    private final String status;

    RequestStatus(String status){
        this.status = status;
    }
}
