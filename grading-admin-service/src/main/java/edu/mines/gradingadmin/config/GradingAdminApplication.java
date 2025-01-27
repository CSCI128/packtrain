package edu.mines.gradingadmin.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class GradingAdminApplication {

	public static void main(String[] args) {
		SpringApplication.run(GradingAdminApplication.class, args);
	}
}
