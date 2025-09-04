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
public class CourseSyncNotificationDTO {

    @JsonProperty("error")
    private String error;

    @JsonProperty("course_complete")
    private Boolean courseComplete;

    @JsonProperty("sections_complete")
    private Boolean sectionsComplete;

    @JsonProperty("assignments_complete")
    private Boolean assignmentsComplete;

    @JsonProperty("members_complete")
    private Boolean membersComplete;
}
