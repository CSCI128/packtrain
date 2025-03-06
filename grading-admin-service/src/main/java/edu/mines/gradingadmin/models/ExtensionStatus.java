package edu.mines.gradingadmin.models;

import lombok.Getter;

import java.util.stream.Stream;

public enum ExtensionStatus {
    APPROVED("approved"),
    DENIED("denied"),
    PENDING("pending");


    private final String status;

    ExtensionStatus(String status){
        this.status = status;
    }
}
