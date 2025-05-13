package edu.mines.packtrain.models.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum LateRequestStatus {
    @JsonProperty("ignored")
    IGNORED("ignored"),
    @JsonProperty("approved")
    APPROVED("approved"),
    @JsonProperty("rejected")
    REJECTED("rejected"),
    @JsonProperty("pending")
    PENDING("pending"),
    @JsonProperty("no_extension")
    NO_EXTENSION("no_extension"),
    @JsonProperty("applied")
    APPLIED("applied");

    private final String status;

    LateRequestStatus(String status){
        this.status = status;
    }
}
