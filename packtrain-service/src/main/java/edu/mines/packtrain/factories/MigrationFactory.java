package edu.mines.packtrain.factories;

import com.rabbitmq.client.Channel;
import edu.mines.packtrain.data.policyServer.GradingStartDTO;
import edu.mines.packtrain.data.policyServer.ScoredDTO;
import edu.mines.packtrain.models.Assignment;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MigrationFactory {

    @Getter
    @Setter(AccessLevel.PRIVATE)
    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ProcessScoresAndExtensionsConfig {
        private UUID migrationId;
        private Channel rawGradePublishChannel;
        private Channel scoreReceivedChannel;

        private GradingStartDTO gradingStartDTO;
    }

    public static class StartProcessScoresAndExtensionsFactory {

        private final Function<String, Optional<Channel>> createScorePublishChannel;
        private final BiFunction<String, Consumer<ScoredDTO>, Optional<Channel>>
                createScoreReceivedChannel;
        private final ProcessScoresAndExtensionsConfig processScoresAndExtensionsConfig;
        private Consumer<ScoredDTO> onScoreReceived;


        StartProcessScoresAndExtensionsFactory(UUID migrationId, Function<String,
                Optional<Channel>> createScorePublishChannel,
                BiFunction<String, Consumer<ScoredDTO>,
                Optional<Channel>> createScoreReceivedChannel) {
            this.createScorePublishChannel = createScorePublishChannel;
            this.createScoreReceivedChannel = createScoreReceivedChannel;
            processScoresAndExtensionsConfig = new ProcessScoresAndExtensionsConfig();
            processScoresAndExtensionsConfig.setMigrationId(migrationId);
            processScoresAndExtensionsConfig.setGradingStartDTO(new GradingStartDTO());
        }


        public StartProcessScoresAndExtensionsFactory forAssignment(Assignment assignment) {
            processScoresAndExtensionsConfig.getGradingStartDTO().setGlobalMetadata(
                    new GradingStartDTO.GlobalAssignmentMetadata(
                    assignment.getId(),
                    assignment.getPoints(),
                    0,
                    assignment.getDueDate()
            ));

            return this;
        }

        public StartProcessScoresAndExtensionsFactory withPolicy(URI policy) {
            processScoresAndExtensionsConfig.getGradingStartDTO().setPolicyURI(policy);
            return this;
        }

        public StartProcessScoresAndExtensionsFactory withOnScoreReceived(Consumer<ScoredDTO>
                                                                                  onScoreReceived) {
            this.onScoreReceived = onScoreReceived;
            return this;
        }


        public ProcessScoresAndExtensionsConfig build() throws IOException, TimeoutException {
            String rawScoreRoutingKey = String.format("%s.raw-grades",
                    processScoresAndExtensionsConfig.getMigrationId());
            Optional<Channel> publishChannel = createScorePublishChannel.apply(rawScoreRoutingKey);
            if (publishChannel.isEmpty()) {
                throw new IOException("Failed to create raw score publish channel!");
            }

            processScoresAndExtensionsConfig.getGradingStartDTO()
                    .setRawGradeRoutingKey(rawScoreRoutingKey);
            processScoresAndExtensionsConfig.setRawGradePublishChannel(publishChannel.get());

            if (onScoreReceived == null) {
                log.warn("No score received action specified!");
                return processScoresAndExtensionsConfig;
            }

            String scoredRoutingKey = String.format("%s.scored",
                    processScoresAndExtensionsConfig.getMigrationId());
            Optional<Channel> scoreReceivedChannel = createScoreReceivedChannel
                    .apply(scoredRoutingKey, onScoreReceived);

            if (scoreReceivedChannel.isEmpty()) {
                publishChannel.get().close();
                throw new IOException("Failed to create score received channel!");
            }

            processScoresAndExtensionsConfig.getGradingStartDTO().setScoreCreatedRoutingKey(scoredRoutingKey);
            processScoresAndExtensionsConfig.setScoreReceivedChannel(scoreReceivedChannel.get());

            return processScoresAndExtensionsConfig;
        }
    }

    public static StartProcessScoresAndExtensionsFactory startProcessScoresAndExtensions(
            UUID migrationId, Function<String, Optional<Channel>> createScorePublishChannel,
            BiFunction<String, Consumer<ScoredDTO>, Optional<Channel>> createScoreReceivedChannel) {

        // in case we need to eventually wrap things from the parent
        return new StartProcessScoresAndExtensionsFactory(migrationId, createScorePublishChannel,
                createScoreReceivedChannel);

    }
}

