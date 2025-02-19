package edu.mines.gradingadmin.services.external;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import edu.mines.gradingadmin.config.ExternalServiceConfig;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

@Service
public class RabbitMqService {
    private final ExternalServiceConfig.RabbitMqConfig rabbitMqConfig;
    private Connection rabbitMqConnection;
    private Channel gradingMessageChannel;


    public RabbitMqService(ExternalServiceConfig.RabbitMqConfig rabbitMqConfig) throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException, IOException, TimeoutException {
        this.rabbitMqConfig = rabbitMqConfig;

        if (!rabbitMqConfig.isEnabled()){
            return;
        }

        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(rabbitMqConfig.getUri());
        rabbitMqConnection = factory.newConnection();
    }

    @EventListener(ContextRefreshedEvent.class)
    public void onApplicationEvent(ContextRefreshedEvent event) throws IOException {
        if (!rabbitMqConfig.isEnabled()){
            return;
        }

        gradingMessageChannel = rabbitMqConnection.createChannel();







    }


}
