package edu.mines.gradingadmin.containers;

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
    Supplier<Object> rabbitMqGradingMessageRoutingKey = () -> "grading-admin.messaging";

    @DynamicPropertySource
    static void setRabbitMqProperties(DynamicPropertyRegistry properties){
        properties.add("grading-admin.external-services.rabbitmq.enabled", () -> true);
        properties.add("grading-admin.external-services.rabbitmq.uri", rabbitMq::getAmqpUrl);
        properties.add("grading-admin.external-services.rabbitmq.exchange-name", rabbitMqExchange);
        properties.add("grading-admin.external-services.rabbitmq.grading-message-routing-key", rabbitMqGradingMessageRoutingKey);

    }
}
