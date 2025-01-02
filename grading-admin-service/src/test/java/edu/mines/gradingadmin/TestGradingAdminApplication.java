package edu.mines.gradingadmin;

import org.springframework.boot.SpringApplication;

public class TestGradingAdminApplication {

	public static void main(String[] args) {
		SpringApplication.from(GradingAdminApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
