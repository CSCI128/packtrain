package edu.mines.gradingadmin.services;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.ResponseEntity;

@ControllerAdvice
public class GlobalExceptionHandlingService {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> handleResponseException(ResponseStatusException exception){
        return ResponseEntity.status(exception.getStatusCode()).body(exception.getReason());
    }

}
