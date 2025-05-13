package edu.mines.packtrain.controllers;

import edu.mines.packtrain.api.ApiDelegate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class HealthApiImpl implements ApiDelegate {
    @Override
    public ResponseEntity<Void> checkHealth() {
        return ResponseEntity.ok().build();
    }
}
