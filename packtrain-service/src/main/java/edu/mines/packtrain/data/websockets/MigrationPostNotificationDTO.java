package edu.mines.packtrain.data.websockets;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MigrationPostNotificationDTO {

    @JsonProperty("error")
    private String error;

    @JsonProperty("post_complete")
    private Boolean post_complete;
}
