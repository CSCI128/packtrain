package edu.mines.gradingadmin.services.external;

public class ExternalServiceDisabledException extends RuntimeException {
    public ExternalServiceDisabledException(String message) {
        super(message);
    }
}
