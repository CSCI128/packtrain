package edu.mines.gradingadmin.services.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import edu.mines.gradingadmin.config.ExternalServiceConfig;
import edu.mines.gradingadmin.data.messages.GradingStartDTO;
import edu.mines.gradingadmin.data.messages.RawGradeDTO;
import edu.mines.gradingadmin.data.messages.ScoredDTO;
import edu.mines.gradingadmin.models.Assignment;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
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
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

@Service
@Slf4j
public class RabbitMqService {
    private final ExternalServiceConfig.RabbitMqConfig rabbitMqConfig;
    private final ObjectMapper mapper;
    private Connection rabbitMqConnection;

    // todo - in the future we should probably move this into the migration service somehow
    @Data
    public static class MigrationConfig{
        private UUID migrationId;
        private Channel rawGradePublishChannel;
        private Channel scoreReceivedChannel;

        private GradingStartDTO gradingStartDTO;
    }

    public static class StartGradeMigrationFactory{
        private final Connection connection;
        private final String exchangeName;
        private final ObjectMapper mapper;
        private MigrationConfig migrationConfig;
        private GradingStartDTO gradingStartDTO;
        private Consumer<ScoredDTO> onScoreReceived;

        public StartGradeMigrationFactory(UUID migrationId, Connection connection, String exchangeName, ObjectMapper mapper) {
            this.connection = connection;
            this.exchangeName = exchangeName;
            this.mapper = mapper;
            migrationConfig = new MigrationConfig();
            migrationConfig.setMigrationId(migrationId);
            gradingStartDTO = new GradingStartDTO();
            gradingStartDTO.setMigrationId(migrationId);
        }

        public StartGradeMigrationFactory forAssignment(Assignment assignment){
            gradingStartDTO.setGlobalMetadata(new GradingStartDTO.GlobalAssignmentMetadata(
                    assignment.getId(),
                    assignment.getPoints(),
                    0,
                    assignment.getDueDate()
            ));

            return this;
        }

        public StartGradeMigrationFactory withPolicy(URI policy){
            gradingStartDTO.setPolicyURI(policy);
            return this;
        }

        public StartGradeMigrationFactory withOnScoreReceived(Consumer<ScoredDTO> onScoreReceived){
            this.onScoreReceived = onScoreReceived;
            return this;
        }

        private Optional<Channel> createRawGradePublishChannel(){
            String routingKey = String.format("%s.raw-grades", migrationConfig.getMigrationId());

            log.info("Creating new grade publish channel for migration '{}' with routing key '{}'", migrationConfig.getMigrationId(), routingKey);
            try {
                Channel publishChannel = connection.createChannel();
                // if the exchange already exists, then nothing happens
                publishChannel.exchangeDeclare(exchangeName, "direct", true);
                String queueName = publishChannel.queueDeclare().getQueue();
                publishChannel.queueBind(queueName, exchangeName, routingKey);

                log.info("Bind raw grade publish queue '{}' on exchange '{}' with routing key '{}'", queueName, exchangeName, routingKey);

                gradingStartDTO.setRawGradeRoutingKey(routingKey);
                return Optional.of(publishChannel);
            } catch (IOException e) {
                log.error("Failed to create raw grade publish channel for migration '{}' with routing key '{}'", migrationConfig.getMigrationId(), routingKey);
                log.error("Failed due to: ", e);
            }

            return Optional.empty();
        }

        private Optional<Channel> createScoreReceivedChannel(){
            String routingKey = String.format("%s.scored", migrationConfig.getMigrationId());

            log.info("Creating new score received channel for migration '{}' with routing key '{}'", migrationConfig.getMigrationId(), routingKey);

            try {
                Channel recievedChannel = connection.createChannel();
                recievedChannel.exchangeDeclare(exchangeName, "direct", true);
                String queueName = recievedChannel.queueDeclare().getQueue();
                recievedChannel.queueBind(queueName, exchangeName, routingKey);

                log.info("Bind score received queue '{}' on exchange '{}' with routing key '{}'", queueName, exchangeName, routingKey);

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

                gradingStartDTO.setScoreCreatedRoutingKey(routingKey);
                return Optional.of(recievedChannel);

            } catch (IOException e) {
                log.error("Failed to create score received channel for migration '{}' with routing key '{}'", migrationConfig.getMigrationId(), routingKey);
                log.error("Failed due to: ", e);
            }

            return Optional.empty();
        }

        public MigrationConfig build() throws IOException, TimeoutException {
            Optional<Channel> publishChannel = createRawGradePublishChannel();
            if (publishChannel.isEmpty()){
                throw new IOException("Failed to create raw score publish channel!");
            }
            Optional<Channel> scoreReceivedChannel = createScoreReceivedChannel();
            if (scoreReceivedChannel.isEmpty()){
                publishChannel.get().close();
                throw new IOException("Failed to create score received channel!");
            }

            migrationConfig.setGradingStartDTO(gradingStartDTO);
            migrationConfig.setRawGradePublishChannel(publishChannel.get());
            migrationConfig.setScoreReceivedChannel(scoreReceivedChannel.get());

            return migrationConfig;
        }

    }

    public RabbitMqService(ExternalServiceConfig.RabbitMqConfig rabbitMqConfig, ObjectMapper mapper) throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException, IOException, TimeoutException {
        this.rabbitMqConfig = rabbitMqConfig;
        this.mapper = mapper;

        if (!rabbitMqConfig.isEnabled()){
            log.warn("RabbitMQ is disabled!");
            return;
        }

        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(rabbitMqConfig.getUri());
        rabbitMqConnection = factory.newConnection();

        log.info("Connected to RabbitMQ broker at '{}' with client id '{}'", rabbitMqConnection.getAddress(), rabbitMqConnection.getId());
    }

    public StartGradeMigrationFactory createMigrationConfig(UUID migrationId){
        return new StartGradeMigrationFactory(migrationId, rabbitMqConnection, rabbitMqConfig.getExchangeName(), mapper);
    }

    public boolean sendScore(MigrationConfig migrationConfig, RawGradeDTO rawGrade){
        log.debug("Sending raw score for '{}' for migration '{}'", rawGrade.getCwid(), migrationConfig.getMigrationId());

        if (!migrationConfig.getRawGradePublishChannel().isOpen()){
            log.warn("Raw grade publish channel for migration '{}' is closed! Attempting to recover", migrationConfig.getMigrationId());
            try {
                migrationConfig.getRawGradePublishChannel().basicRecover(true);
            } catch (IOException e) {
                log.error("Failed to recover connection!", e);
                return false;
            }
        }

        try {
            migrationConfig.getRawGradePublishChannel().basicPublish(
                    rabbitMqConfig.getExchangeName(),
                    migrationConfig.getGradingStartDTO().getRawGradeRoutingKey(),
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
