package edu.mines.gradingadmin;

import edu.mines.gradingadmin.config.GradingAdminApplication;
import edu.mines.gradingadmin.containers.PostgresTestContainer;
import org.springframework.boot.SpringApplication;

public class TestGradingAdminApplication {

	public static void main(String[] args) {
		SpringApplication.from(GradingAdminApplication::main).with(PostgresTestContainer.class).run(args);
	}

}
