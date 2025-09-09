package edu.mines.packtrain.config;

import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.mailer.MailerBuilder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SmtpConfig {
    private final String smtpServer;
    private final int smtpPort;
    private final String smtpUsername;
    private final String smtpPassword;

    public SmtpConfig(
            @Value("${grading-admin.email.smtp-server}") String smtpServer,
            @Value("${grading-admin.email.smtp-port}") int smtpPort,
            @Value("${grading-admin.email.smtp-username}") String smtpUsername,
            @Value("${grading-admin.email.smtp-password}") String smtpPassword) {
        this.smtpServer = smtpServer;
        this.smtpPort = smtpPort;
        this.smtpUsername = smtpUsername;
        this.smtpPassword = smtpPassword;
    }

    @Bean
    public Mailer createMailer() {
        return MailerBuilder
                .withSMTPServerHost(smtpServer)
                .withSMTPServerPort(smtpPort)
                .withSMTPServerUsername(smtpUsername)
                .withSMTPServerPassword(smtpPassword)
                .buildMailer();
    }

}
