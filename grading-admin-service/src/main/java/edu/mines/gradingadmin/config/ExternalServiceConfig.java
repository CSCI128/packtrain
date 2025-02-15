package edu.mines.gradingadmin.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.util.InvalidPropertiesFormatException;

@Configuration
public class ExternalServiceConfig {
    @AllArgsConstructor
    @Getter
    public static class CanvasConfig{
        private URI endpoint;
        private String teacherEnrollment;
        private String studentEnrollment;
        private String taEnrollment;
    }


    @AllArgsConstructor
    @Getter
    public static class S3Config{
        private URI endpoint;
        private String accessKey;
        private String secretKey;
    }

    @AllArgsConstructor
    @Getter
    public static class RabbitMqConfig{
        private URI uri;
    };

    @Bean
    public CanvasConfig configureCanvas(
            @Value("${grading-admin.external-services.canvas.endpoint}") URI endpoint,
            @Value("${grading-admin.external-services.canvas.teacher-enrollment-name}") String teacherEnrollment,
            @Value("${grading-admin.external-services.canvas.student-enrollment-name}") String studentEnrollment,
            @Value("${grading-admin.external-services.canvas.ta-enrollment-name}") String taEnrollment
    ) throws InvalidPropertiesFormatException {
        if (endpoint == null){
            throw new InvalidPropertiesFormatException("Canvas endpoint not defined");
        }
        return new CanvasConfig(endpoint, teacherEnrollment, studentEnrollment, taEnrollment);
    }

    @Bean
    public S3Config configureS3(
            @Value("${grading-admin.external-services.s3.uri}") URI endpoint,
            @Value("${grading-admin.external-services.s3.access_key}") String accessKey,
            @Value("${grading-admin.external-services.s3.secret_key}") String secretKey
    ){
        return new S3Config(endpoint, accessKey, secretKey);
    }

    @Bean
    public RabbitMqConfig configRabbitMq(
            @Value("${grading-admin.external-services.rabbitmq.uri}") URI uri
    ){
        return new RabbitMqConfig(uri);
    }



}
