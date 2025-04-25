package edu.mines.gradingadmin.controllers;

import edu.mines.gradingadmin.data.ErrorResponseDTO;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.ResponseEntity;

@ControllerAdvice
public class GlobalExceptionHandlingService {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponseDTO> handleResponseException(ResponseStatusException exception){
        return ResponseEntity.status(exception.getStatusCode()).body(new ErrorResponseDTO().errorMessage(exception.getMessage()));
    }

}
