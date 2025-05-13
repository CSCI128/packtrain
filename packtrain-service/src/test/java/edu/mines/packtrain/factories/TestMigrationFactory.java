package edu.mines.packtrain.factories;

import com.rabbitmq.client.Channel;
import edu.mines.packtrain.data.policyServer.ScoredDTO;
import edu.mines.packtrain.models.Assignment;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class TestMigrationFactory {

    @Test
    @SneakyThrows
    void verifyCreateMigrationConfig() {
        Assignment assignment = new Assignment();
        assignment.setId(UUID.randomUUID());
        assignment.setPoints(10);
        assignment.setDueDate(Instant.now());

        URI uri = URI.create("http://localhost:3000/policy.js");

        Consumer<ScoredDTO> onReceived = Mockito.mock(Consumer.class);

        Channel ch = Mockito.mock(Channel.class);

        Function<String, Optional<Channel>> createPublish = Mockito.mock(Function.class);
        Mockito.when(createPublish.apply(Mockito.anyString())).thenReturn(Optional.of(ch));

        BiFunction<String, Consumer<ScoredDTO>, Optional<Channel>> createReceive = Mockito.mock(BiFunction.class);
        Mockito.when(createReceive.apply(Mockito.anyString(), Mockito.any())).thenReturn(Optional.of(ch));

        MigrationFactory.ProcessScoresAndExtensionsConfig config = MigrationFactory.startProcessScoresAndExtensions(UUID.randomUUID(), createPublish, createReceive)
                .forAssignment(assignment)
                .withPolicy(uri)
                .withOnScoreReceived(onReceived)
                .build();


        Mockito.verify(createPublish, Mockito.atLeastOnce()).apply(Mockito.anyString());
        Mockito.verify(createReceive, Mockito.atLeastOnce()).apply(Mockito.anyString(), Mockito.any());

        Assertions.assertEquals(assignment.getId(), config.getGradingStartDTO().getGlobalMetadata().getAssignmentId());
        Assertions.assertEquals(uri, config.getGradingStartDTO().getPolicyURI());
        Assertions.assertNotNull(config.getGradingStartDTO().getScoreCreatedRoutingKey());
        Assertions.assertNotNull(config.getGradingStartDTO().getRawGradeRoutingKey());
    }

    @Test
    @SneakyThrows
    void verifyCreateMigrationFailsWhenChannelFails() {
        Assignment assignment = new Assignment();
        assignment.setId(UUID.randomUUID());
        assignment.setPoints(10);
        assignment.setDueDate(Instant.now());

        URI uri = URI.create("http://localhost:3000/policy.js");

        Consumer<ScoredDTO> onReceived = Mockito.mock(Consumer.class);

        Channel ch = Mockito.mock(Channel.class);

        Function<String, Optional<Channel>> createPublish = Mockito.mock(Function.class);
        Mockito.when(createPublish.apply(Mockito.anyString())).thenReturn(Optional.of(ch));

        BiFunction<String, Consumer<ScoredDTO>, Optional<Channel>> createReceive = Mockito.mock(BiFunction.class);
        Mockito.when(createReceive.apply(Mockito.anyString(), Mockito.any())).thenReturn(Optional.empty());

        Assertions.assertThrows(IOException.class, () ->
                MigrationFactory.startProcessScoresAndExtensions(UUID.randomUUID(), createPublish, createReceive)
                        .withOnScoreReceived(onReceived)
                        .build()
        );

        Mockito.verify(createPublish, Mockito.atLeastOnce()).apply(Mockito.anyString());
        Mockito.verify(createReceive, Mockito.atLeastOnce()).apply(Mockito.anyString(), Mockito.any());

    }

    @Test
    @SneakyThrows
    void verifyCreateMigrationWithNoReceiver(){
        Assignment assignment = new Assignment();
        assignment.setId(UUID.randomUUID());
        assignment.setPoints(10);
        assignment.setDueDate(Instant.now());

        URI uri = URI.create("http://localhost:3000/policy.js");

        Channel ch = Mockito.mock(Channel.class);

        Function<String, Optional<Channel>> createPublish = Mockito.mock(Function.class);
        Mockito.when(createPublish.apply(Mockito.anyString())).thenReturn(Optional.of(ch));

        BiFunction<String, Consumer<ScoredDTO>, Optional<Channel>> createReceive = Mockito.mock(BiFunction.class);
        Mockito.when(createReceive.apply(Mockito.anyString(), Mockito.any())).thenReturn(Optional.of(ch));

        MigrationFactory.ProcessScoresAndExtensionsConfig config = MigrationFactory.startProcessScoresAndExtensions(UUID.randomUUID(), createPublish, createReceive)
                .forAssignment(assignment)
                .withPolicy(uri)
                .build();

        Mockito.verify(createPublish, Mockito.atLeastOnce()).apply(Mockito.anyString());
        Mockito.verify(createReceive, Mockito.never()).apply(Mockito.anyString(), Mockito.any());

        Assertions.assertNotNull(config.getGradingStartDTO().getRawGradeRoutingKey());
        Assertions.assertNull(config.getGradingStartDTO().getScoreCreatedRoutingKey());
    }

}
