package edu.mines.gradingadmin.models;

public enum CredentialType {
    CANVAS("canvas"), GRADESCOPE("gradescope");

    private final String type;

    CredentialType(String type) {
        this.type = type;
    }
}
