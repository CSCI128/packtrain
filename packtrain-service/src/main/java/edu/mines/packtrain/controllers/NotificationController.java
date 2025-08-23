package edu.mines.packtrain.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
public class NotificationController {

    @MessageMapping("/sendMessage")
    @SendTo("/topic/messages")
    public String sendMessage(String message) {
        System.out.println("Received: " + message);
        return message; // broadcast back
    }
}
