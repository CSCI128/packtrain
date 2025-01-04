package edu.mines.gradingadmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class GradingAdminApplication {

	public static void main(String[] args) {
		SpringApplication.run(GradingAdminApplication.class, args);
	}

	@GetMapping(value = "/", produces = "text/plain")
	public String home(String name) {
		return "Hello!";
	}

	@GetMapping("/hello")
	public String hello(@RequestParam(value = "name", defaultValue = "World") String name) {
		return String.format("Hello %s!", name);
	}
}
