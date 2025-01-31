package edu.mines.gradingadmin.config;

import edu.mines.gradingadmin.models.ExternalSource;
import edu.mines.gradingadmin.models.ExternalSourceType;
import edu.mines.gradingadmin.repositories.ExternalSourceRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.EventListener;

import java.net.URL;

@Configuration
@Slf4j
public class EndpointConfiguration {
    private final ExternalSourceRepo externalSourceRepo;

    @Value("${grading-admin.endpoints.canvas}")
    URL canvasEndpoint;

    public EndpointConfiguration(ExternalSourceRepo externalSourceRepo) {
        this.externalSourceRepo = externalSourceRepo;
    }


    @EventListener(ContextStartedEvent.class)
    public void createEndpoints(){
        // todo: may want to make this more dynamic
        // todo: may want to move this into a service

        // configure canvas - I really don't like this
        if (!externalSourceRepo.existsByEndpoint(canvasEndpoint.toString())){
            ExternalSource source = new ExternalSource();
            source.setType(ExternalSourceType.CANVAS);
            source.setEndpoint(canvasEndpoint.toString());
            source.setActive(true);

            externalSourceRepo.save(source);
        }

    }





}
