package edu.mines.gradingadmin.services.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import edu.mines.gradingadmin.containers.PostgresTestContainer;
import edu.mines.gradingadmin.containers.RabbitMqTestContainer;
import edu.mines.gradingadmin.data.messages.GradingStartDTO;
import edu.mines.gradingadmin.data.messages.RawGradeDTO;
import edu.mines.gradingadmin.data.messages.ScoredDTO;
import edu.mines.gradingadmin.factories.MigrationFactory;
import edu.mines.gradingadmin.models.Assignment;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;


@SpringBootTest
public class TestRabbitMqService implements PostgresTestContainer, RabbitMqTestContainer {
    private static Connection rabbitMqConnection;

    @Autowired
    private RabbitMqService rabbitMqService;

    @Autowired
    private ObjectMapper mapper;


    @BeforeAll
    static void setupClass() throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException, IOException, TimeoutException {
        postgres.start();
        rabbitMq.start();

        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(rabbitMq.getAmqpUrl());

        rabbitMqConnection = factory.newConnection();
    }

    @AfterAll
    static void tearDownClass() throws IOException {
        rabbitMqConnection.close();

    }

    @Test
    void verifyReceiveScore() throws IOException, TimeoutException, InterruptedException {
        Consumer<ScoredDTO> onReceived = Mockito.mock(Consumer.class);

        String routingKey = UUID.randomUUID().toString();

        Channel consumer = rabbitMqService.createScoreReceivedChannel(routingKey, onReceived).orElseThrow(AssertionError::new);

        Channel scoreSender = rabbitMqConnection.createChannel();
        String queue = scoreSender.queueDeclare().getQueue();
        scoreSender.queueBind(queue, rabbitMqExchange.get().toString(), routingKey);

        ScoredDTO scored = new ScoredDTO();

        scoreSender.basicPublish(
                rabbitMqExchange.get().toString(),
                routingKey,
                new AMQP.BasicProperties.Builder()
                        .contentType("application/json")
                        .build(),
                mapper.writeValueAsBytes(scored));

        // Yucky - but not really a good way to wait for the network otherwise
        TimeUnit.MILLISECONDS.sleep(500);

        // we are only verifying that our code got called, we are assuming the rabbitmq can send JSON correctly
        Mockito.verify(onReceived, Mockito.atLeastOnce()).accept(Mockito.any());

        // clean up testing connection
        scoreSender.close();
        // clean up consumer
        consumer.close();
    }

    @Test
    void verifySendRawScore() throws IOException, TimeoutException, InterruptedException {
        String routingKey = UUID.randomUUID().toString();
        Channel publisher = rabbitMqService.createRawGradePublishChannel(routingKey).orElseThrow(AssertionError::new);

        Channel scoreReceiver = rabbitMqConnection.createChannel();
        String queue = scoreReceiver.queueDeclare().getQueue();
        scoreReceiver.queueBind(queue, rabbitMqExchange.get().toString(), routingKey);

        class Wrapper {
            RawGradeDTO data;
        }

        Wrapper wrapper = new Wrapper();

        scoreReceiver.basicConsume(queue, new DefaultConsumer(scoreReceiver) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                wrapper.data = mapper.readValue(body, RawGradeDTO.class);
            }
        });

        RawGradeDTO score = new RawGradeDTO();
        score.setCwid("99");

        Assertions.assertTrue(rabbitMqService.sendScore(publisher, routingKey, score));

        // Yucky - but not really a good way to wait for the network otherwise
        TimeUnit.MILLISECONDS.sleep(500);

        Assertions.assertNotNull(wrapper.data);
        Assertions.assertEquals("99", wrapper.data.getCwid());

        scoreReceiver.close();
        publisher.close();
    }

}
