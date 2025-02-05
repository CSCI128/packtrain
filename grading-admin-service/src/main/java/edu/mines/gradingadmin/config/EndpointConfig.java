package edu.mines.gradingadmin.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.util.InvalidPropertiesFormatException;

@Configuration
public class EndpointConfig {
    @AllArgsConstructor
    @Getter
    public static class CanvasConfig{
        private URI endpoint;
    }

    @Bean
    public CanvasConfig configureCanvas(
            @Value("${grading-admin.external-services.canvas.endpoint}") URI endpoint
    ) throws InvalidPropertiesFormatException {
        if (endpoint == null){
            throw new InvalidPropertiesFormatException("Canvas endpoint not defined");
        }
        return new CanvasConfig(endpoint);
    }

}
