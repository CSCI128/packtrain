package edu.mines.gradingadmin.services.external;

import edu.mines.gradingadmin.config.ExternalServiceConfig;
import edu.mines.gradingadmin.data.messages.GradingStartDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class PolicyServerService {
    private final ExternalServiceConfig.PolicyServerConfig config;

    private RestClient client;

    public PolicyServerService(ExternalServiceConfig.PolicyServerConfig config) {
        this.config = config;

        if (!this.config.isEnabled()) {
            log.warn("Policy server is disabled!");
            return;
        }

        client = RestClient.builder()
                .baseUrl(this.config.getUri())
                .build();
    }

    private boolean serverReady(){
        ResponseEntity<Void> res = client.get().uri("/-/ready").retrieve().toBodilessEntity();

        return res.getStatusCode() == HttpStatus.OK;
    }

    public void startGrading(GradingStartDTO gradingStartDTO){
        if (!config.isEnabled()){
            throw new ExternalServiceDisabledException("Policy Server Service");
        }
        if (!serverReady()){
            log.warn("Failed to start grading - policy server '{}' is not ready!", config.getUri());
        }

        ResponseEntity<String> res = client.post().uri("/grading/start").body(gradingStartDTO).retrieve().toEntity(String.class);

        if (res.getStatusCode() != HttpStatus.CREATED){
            log.error("Failed to start grading! Due to: {}", res);
            return;
        }

        log.info("Policy server is ready for grading: {}", res.getBody());
    }


}
