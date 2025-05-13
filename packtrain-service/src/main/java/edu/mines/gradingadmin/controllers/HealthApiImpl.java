package edu.mines.gradingadmin.controllers;

import edu.mines.gradingadmin.api.ApiDelegate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class HealthApiImpl implements ApiDelegate {
    @Override
    public ResponseEntity<Void> checkHealth() {
        return ResponseEntity.ok().build();
    }
}
