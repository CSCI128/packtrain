package edu.mines.gradingadmin.containers;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.function.Supplier;


@Testcontainers
public interface PostgresTestContainer {
	PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:alpine"));

	Supplier<Object> CONNECTION_STRING = postgres::getJdbcUrl;

	Supplier<Object> USER_NAME = postgres::getUsername;

	Supplier<Object> PASSWORD = postgres::getPassword;

	@DynamicPropertySource
	static void setProperties(DynamicPropertyRegistry registry){
		registry.add("spring.datasource.url", CONNECTION_STRING);
		registry.add("spring.datasource.username", USER_NAME);
		registry.add("spring.datasource.password", PASSWORD);
	}


}