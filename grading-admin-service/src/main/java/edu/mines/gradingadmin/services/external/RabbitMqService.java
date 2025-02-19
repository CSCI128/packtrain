package edu.mines.gradingadmin.services.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import edu.mines.gradingadmin.config.ExternalServiceConfig;
import edu.mines.gradingadmin.data.messages.ScoredDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.startup.ContextConfig;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

@Service
@Slf4j
public class RabbitMqService {
    private final ExternalServiceConfig.RabbitMqConfig rabbitMqConfig;
    private final ObjectMapper jacksonObjectMapper;
    private final ConcurrentMap<UUID, Channel> rawGradePublishChannels;
    private final ConcurrentMap<UUID, Channel> scoreReceiveChannels;
    private Connection rabbitMqConnection;
    private Channel gradingMessageChannel;



    public RabbitMqService(ExternalServiceConfig.RabbitMqConfig rabbitMqConfig, ObjectMapper jacksonObjectMapper) throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException, IOException, TimeoutException {
        this.rabbitMqConfig = rabbitMqConfig;
        this.jacksonObjectMapper = jacksonObjectMapper;

        rawGradePublishChannels = new ConcurrentHashMap<>();
        scoreReceiveChannels = new ConcurrentHashMap<>();

        if (!rabbitMqConfig.isEnabled()){
            log.warn("RabbitMQ is disabled!");
            return;
        }

        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(rabbitMqConfig.getUri());
        rabbitMqConnection = factory.newConnection();

        log.info("Connected to RabbitMQ broker at '{}' with client id '{}'", rabbitMqConnection.getAddress(), rabbitMqConnection.getId());
    }

    @EventListener(ContextRefreshedEvent.class)
    public void onContextRefreshed(ContextRefreshedEvent event) throws IOException {
        if (!rabbitMqConfig.isEnabled()){
            return;
        }

        gradingMessageChannel = rabbitMqConnection.createChannel();

        gradingMessageChannel.exchangeDeclare(rabbitMqConfig.getExchangeName(), "direct", true);
        String queueName = gradingMessageChannel.queueDeclare().getQueue();

        gradingMessageChannel.queueBind(queueName, rabbitMqConfig.getExchangeName(), rabbitMqConfig.getGradingMessageRoutingKey());

        log.info("Bind grading message queue '{}' on exchange '{}' with routing key '{}'", queueName, rabbitMqConfig.getExchangeName(), rabbitMqConfig.getGradingMessageRoutingKey());

    }

    @EventListener(ContextClosedEvent.class)
    public void onContextClosed(ContextClosedEvent event){
        if (!rabbitMqConfig.isEnabled()){
            return;
        }

        if (gradingMessageChannel != null && gradingMessageChannel.isOpen()) {
            try {
                gradingMessageChannel.close();
            } catch (IOException | TimeoutException e) {
                log.error("Failed to close grading message channel!", e);
            }
        }

        try {
            rabbitMqConnection.close();
        } catch (IOException e) {
            log.error("Failed to close rabbitMQ connection!", e);
        }

    }

    private Optional<String> createRawGradePublishChannel(UUID migrationId){
        String routingKey = String.format("%s.raw-grades", migrationId);

        log.info("Creating new grade publish channel for migration '{}' with routing key '{}'", migrationId, routingKey);
        try {
            Channel publishChannel = rabbitMqConnection.createChannel();
            String queueName = publishChannel.queueDeclare().getQueue();
            publishChannel.queueBind(queueName, rabbitMqConfig.getExchangeName(), routingKey);

            log.info("Bind raw grade publish queue '{}' on exchange '{}' with routing key '{}'", queueName, rabbitMqConfig.getExchangeName(), routingKey);

            rawGradePublishChannels.put(migrationId, publishChannel);
        } catch (IOException e) {
            log.error("Failed to create raw grade publish channel for migration '{}' with routing key '{}'", migrationId, routingKey);
            log.error("Failed due to: ", e);
            return Optional.empty();
        }

        return Optional.of(routingKey);
    }


    public void issueGradingStartMessage(UUID migrationId, URI policyURI, UUID assignmentId, double maxScore, double minScore){

    }

    private void onScoreReceived(UUID migrationId, String routingKey, Consumer<ScoredDTO> onReceived){
        if(!scoreReceiveChannels.containsKey(migrationId)){
            log.error("Attempt to bind consumer to non existent channel! No score receive channel has been defined for migration '{}'!", migrationId);
            throw new RuntimeException("Attempt to bind consumer to non existent channel!");
        }

        Channel recievedChannel = scoreReceiveChannels.get(migrationId);


        try {
            String queueName = recievedChannel.queueDeclare().getQueue();
            recievedChannel.queueBind(queueName, rabbitMqConfig.getExchangeName(), routingKey);
            recievedChannel.basicConsume(queueName, false, new DefaultConsumer(recievedChannel){
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    long deliveryTag = envelope.getDeliveryTag();

                    ScoredDTO parsedBody = jacksonObjectMapper.readValue(body, ScoredDTO.class);

                    onReceived.accept(parsedBody);

                    recievedChannel.basicAck(deliveryTag, false);
                }
            });

        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }











}
