package edu.mines.gradingadmin.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

@Configuration
public class ExternalServiceConfig {
    @AllArgsConstructor
    @RequiredArgsConstructor
    @Getter
    public static class CanvasConfig{
        private final boolean enabled;
        private URI endpoint;
        private String teacherEnrollment;
        private String studentEnrollment;
        private String taEnrollment;
    }


    @AllArgsConstructor
    @RequiredArgsConstructor
    @Getter
    public static class S3Config{
        private final boolean enabled;
        private URI endpoint;
        private String accessKey;
        private String secretKey;
    }

    @AllArgsConstructor
    @RequiredArgsConstructor
    @Getter
    public static class RabbitMqConfig{
        private final boolean enabled;
        private URI uri;
        private String exchangeName;
        private String gradingMessageRoutingKey;
    };

    @Bean
    public CanvasConfig configureCanvas(
            @Value("${grading-admin.external-services.canvas.enabled}") boolean enabled,
            @Value("${grading-admin.external-services.canvas.endpoint}") URI endpoint,
            @Value("${grading-admin.external-services.canvas.teacher-enrollment-name}") String teacherEnrollment,
            @Value("${grading-admin.external-services.canvas.student-enrollment-name}") String studentEnrollment,
            @Value("${grading-admin.external-services.canvas.ta-enrollment-name}") String taEnrollment
    ) {
        if (!enabled){
            return new CanvasConfig(false);
        }
        return new CanvasConfig(true, endpoint, teacherEnrollment, studentEnrollment, taEnrollment);
    }

    @Bean
    public S3Config configureS3(
            @Value("${grading-admin.external-services.s3.enabled}") boolean enabled,
            @Value("${grading-admin.external-services.s3.uri}") URI endpoint,
            @Value("${grading-admin.external-services.s3.access-key}") String accessKey,
            @Value("${grading-admin.external-services.s3.secret-key}") String secretKey
    ){
        if (!enabled){
            return new S3Config(false);
        }
        return new S3Config(true, endpoint, accessKey, secretKey);
    }

    @Bean
    public RabbitMqConfig configRabbitMq(
            @Value("${grading-admin.external-services.rabbitmq.enabled}") boolean enabled,
            @Value("${grading-admin.external-services.rabbitmq.uri}") URI uri,
            @Value("${grading-admin.external-services.rabbitmq.exchange-name}") String exchangeName,
            @Value("${grading-admin.external-services.rabbitmq.grading-message-routing-key}") String gradingMessageQueueName
    ){
        if (!enabled){
            return new RabbitMqConfig(false);
        }
        return new RabbitMqConfig(true, uri, exchangeName, gradingMessageQueueName);
    }



}
