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

    public static ExtensionStatus fromString(String type){
        return Stream.of(ExtensionStatus.values()).filter(r -> r.status.equals(type)).findFirst().orElseThrow(RuntimeException::new);
    }
}
