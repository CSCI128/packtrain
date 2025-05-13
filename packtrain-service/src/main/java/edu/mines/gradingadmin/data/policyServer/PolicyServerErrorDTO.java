package edu.mines.gradingadmin.data.policyServer;

import lombok.Data;

@Data
public class PolicyServerErrorDTO {
    private String status;
    private String reason;
}
