package edu.mines.gradingadmin.services;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import edu.mines.gradingadmin.config.ExternalServiceConfig;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

@Service
public class RabbitMqService {
    private final ExternalServiceConfig.RabbitMqConfig rabbitMqConfig;
    private final Connection rabbitMqConnection;

    public RabbitMqService(ExternalServiceConfig.RabbitMqConfig rabbitMqConfig) throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException, IOException, TimeoutException {
        this.rabbitMqConfig = rabbitMqConfig;
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(rabbitMqConfig.getUri());
        rabbitMqConnection = factory.newConnection();
    }

}
