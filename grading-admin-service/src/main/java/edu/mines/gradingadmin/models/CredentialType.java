package edu.mines.gradingadmin.models;

import java.util.stream.Stream;

public enum CredentialType {
    CANVAS("canvas"), GRADESCOPE("gradescope");

    private final String type;

    CredentialType(String type) {
        this.type = type;
    }

    public static CredentialType fromString(String type){
        return Stream.of(CredentialType.values()).filter(t -> t.type.equals(type)).findFirst().orElseThrow(RuntimeException::new);
    }
}
