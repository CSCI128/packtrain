package edu.mines.packtrain.services.external;

public class ExternalServiceDisabledException extends RuntimeException {
    public ExternalServiceDisabledException(String message) {
        super(message);
    }
}
