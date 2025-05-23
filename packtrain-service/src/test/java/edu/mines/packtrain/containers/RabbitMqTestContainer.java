package edu.mines.packtrain.containers;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.function.Supplier;

@Testcontainers
public interface RabbitMqTestContainer {
    RabbitMQContainer rabbitMq = new RabbitMQContainer(DockerImageName.parse("rabbitmq:4-alpine"));

    Supplier<Object> rabbitMqExchange = () -> "exchange";

    @DynamicPropertySource
    static void setRabbitMqProperties(DynamicPropertyRegistry properties){
        properties.add("grading-admin.external-services.rabbitmq.enabled", () -> true);
        properties.add("grading-admin.external-services.rabbitmq.uri", rabbitMq::getAmqpUrl);
        properties.add("grading-admin.external-services.rabbitmq.user", rabbitMq::getAdminUsername);
        properties.add("grading-admin.external-services.rabbitmq.password", rabbitMq::getAdminPassword);
        properties.add("grading-admin.external-services.rabbitmq.exchange-name", rabbitMqExchange);

    }
}
