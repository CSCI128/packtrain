package edu.mines.gradingadmin.services.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.mines.gradingadmin.config.ExternalServiceConfig;
import edu.mines.gradingadmin.data.policyServer.GradingStartDTO;
import edu.mines.gradingadmin.data.policyServer.PolicyServerErrorDTO;
import edu.mines.gradingadmin.data.policyServer.ValidateDTO;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class PolicyServerService {
    private final ExternalServiceConfig.PolicyServerConfig config;
    private final ObjectMapper objectMapper;

    private RestClient client;

    public PolicyServerService(ExternalServiceConfig.PolicyServerConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;

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

    public boolean startGrading(GradingStartDTO gradingStartDTO){
        if (!config.isEnabled()){
            throw new ExternalServiceDisabledException("Policy Server Service");
        }
        if (!serverReady()){
            log.warn("Failed to start grading - policy server '{}' is not ready!", config.getUri());
        }

        ResponseEntity<String> res = client.post().uri("/grading/start").body(gradingStartDTO).retrieve().toEntity(String.class);

        if (res.getStatusCode() != HttpStatus.CREATED){
            log.error("Failed to start grading! Due to: {}", res);
            return false;
        }

        log.info("Policy server is ready for grading: {}", res.getBody());
        return true;
    }

    public Optional<String> validatePolicy(String policyURI){
        if (!config.isEnabled()){
            throw new ExternalServiceDisabledException("Policy Server Service");
        }

        if (!serverReady()){
            log.warn("Failed to validate policy - policy server '{}' is not ready!", config.getUri());
        }

        log.debug("Validating policy '{}'", policyURI);

        ValidateDTO dto = new ValidateDTO();
        dto.setPolicyURI(policyURI);

        class Container{
            String res = null;
        };
        Container container = new Container();

        ResponseEntity<Void> res = client.post().uri("/validate").body(dto).retrieve()
                .onStatus(HttpStatusCode::isError,
                        (_, response) ->
                                container.res = new String(response.getBody().readAllBytes()))
                .toBodilessEntity();

        return Optional.ofNullable(container.res);
    }


}
