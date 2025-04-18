package edu.mines.gradingadmin.services.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import edu.mines.gradingadmin.config.ExternalServiceConfig;
import edu.mines.gradingadmin.data.policyServer.RawGradeDTO;
import edu.mines.gradingadmin.data.policyServer.ScoredDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

@Service
@Slf4j
public class RabbitMqService {
    private final ExternalServiceConfig.RabbitMqConfig rabbitMqConfig;
    private final ObjectMapper mapper;
    private Connection rabbitMqConnection;

    public RabbitMqService(ExternalServiceConfig.RabbitMqConfig rabbitMqConfig, ObjectMapper mapper) throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException, TimeoutException {
        this.rabbitMqConfig = rabbitMqConfig;
        this.mapper = mapper;

        if (!rabbitMqConfig.isEnabled()){
            log.warn("RabbitMQ is disabled!");
            return;
        }

        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(rabbitMqConfig.getUri());
        factory.setUsername(rabbitMqConfig.getUsername());
        factory.setPassword(rabbitMqConfig.getPassword());

        try {
            rabbitMqConnection = factory.newConnection();
        }catch (IOException e){
            log.error("Failed to connect to RabbitMQ!", e);
            log.warn("RabbitMQ is disabled!");
            return;
        }

        log.info("Connected to RabbitMQ broker at '{}' with client id '{}'", rabbitMqConnection.getAddress(), rabbitMqConnection.getId());
    }

    public Optional<Channel> createRawGradePublishChannel(String routingKey){
        log.info("Creating new grade publish channel for migration with routing key '{}'", routingKey);
        try {
            Channel publishChannel = rabbitMqConnection.createChannel();
            // if the exchange already exists, then nothing happens
            publishChannel.exchangeDeclare(rabbitMqConfig.getExchangeName(), "direct", true);
            String queueName = publishChannel.queueDeclare().getQueue();
            publishChannel.queueBind(queueName, rabbitMqConfig.getExchangeName(), routingKey);

            log.info("Bind raw grade publish queue '{}' on exchange '{}' with routing key '{}'", queueName, rabbitMqConfig.getExchangeName(), routingKey);

            return Optional.of(publishChannel);
        } catch (IOException e) {
            log.error("Failed to create raw grade publish channel for migration with routing key '{}'", routingKey);
            log.error("Failed due to: ", e);
        }

        return Optional.empty();
    }

    public Optional<Channel> createScoreReceivedChannel(String routingKey, Consumer<ScoredDTO> onScoreReceived){
        log.info("Creating new score received channel for migration with routing key '{}'", routingKey);

        try {
            Channel recievedChannel = rabbitMqConnection.createChannel();
            recievedChannel.exchangeDeclare(rabbitMqConfig.getExchangeName(), "direct", true);
            String queueName = recievedChannel.queueDeclare().getQueue();
            recievedChannel.queueBind(queueName, rabbitMqConfig.getExchangeName(), routingKey);

            log.info("Bind score received queue '{}' on exchange '{}' with routing key '{}'", queueName, rabbitMqConfig.getExchangeName(), routingKey);

            recievedChannel.basicConsume(queueName, false, new DefaultConsumer(recievedChannel){
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    long deliveryTag = envelope.getDeliveryTag();

                    ScoredDTO parsedBody = mapper.readValue(body, ScoredDTO.class);
                    log.debug("Received: {}", parsedBody);

                    onScoreReceived.accept(parsedBody);

                    recievedChannel.basicAck(deliveryTag, false);
                }
            });

            log.info("Bind consumer to queue '{}'", queueName);

            return Optional.of(recievedChannel);

        } catch (IOException e) {
            log.error("Failed to create score received channel for migration with routing key '{}'", routingKey);
            log.error("Failed due to: ", e);
        }

        return Optional.empty();
    }

    public boolean sendScore(Channel channel, String routingKey, RawGradeDTO rawGrade){
        log.debug("Sending raw score for '{}' on '{}'", rawGrade.getCwid(), routingKey);

        if (!channel.isOpen()){
            log.warn("Raw grade publish channel is closed! Attempting to recover");
            try {
                channel.basicRecover(true);
            } catch (IOException e) {
                log.error("Failed to recover connection!", e);
                return false;
            }
        }

        try {
            channel.basicPublish(
                    rabbitMqConfig.getExchangeName(),
                    routingKey,
                    new AMQP.BasicProperties().builder()
                            .contentType("application/json")
                            // use persistent mode so that the message can be recovered
                            .deliveryMode(2)
                            .type("grading.raw_score")
                            .build(),
                    mapper.writeValueAsBytes(rawGrade)
            );
        } catch (IOException e) {
            log.error("Failed to send raw score!", e);
            return false;
        }

        return true;
    }
}
