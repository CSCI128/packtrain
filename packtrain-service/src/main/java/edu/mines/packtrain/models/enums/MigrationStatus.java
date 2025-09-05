package edu.mines.packtrain.models.enums;

import java.util.stream.Stream;
import lombok.Getter;

@Getter
public enum MigrationStatus {
    CREATED("created"),
    STARTED("started"),
    AWAITING_REVIEW("awaiting_review"),
    READY_TO_POST("ready_to_post"),
    POSTING("posting"),
    LOADED("loaded"),
    COMPLETED("completed");

    private final String status;

    MigrationStatus(String status) {
        this.status = status;
    }

    public static MigrationStatus fromString(String status) {
        return Stream.of(MigrationStatus.values())
                .filter(r -> r.status.equals(status))
                .findFirst()
                .orElseThrow(RuntimeException::new);
    }
}
