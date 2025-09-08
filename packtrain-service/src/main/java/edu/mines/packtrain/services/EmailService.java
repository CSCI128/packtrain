package edu.mines.packtrain.services;

import java.util.List;

import org.simplejavamail.api.email.EmailPopulatingBuilder;
import org.simplejavamail.api.email.Recipient;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.email.EmailBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.mail.Message;

@Service
public class EmailService {
    private final Mailer mailer;
    private final String fromName;
    private final String fromEmail;

    public EmailService(
            Mailer mailer,
            @Value("${grading-admin.email.from-email}") String fromEmail,
            @Value("${grading-admin.email.from-name}") String fromName) {

        this.mailer = mailer;
        this.fromName = fromName;
        this.fromEmail = fromEmail;
    }


    public void sendEmail(String to, List<String> cc, String text){
        EmailPopulatingBuilder builder = EmailBuilder.startingBlank()
            .from(fromName, fromEmail)
            .to(to)
            .withSubject("Packtrain Notification")
            .withPlainText(text);

        cc.forEach(s -> builder.cc(s));

        mailer.sendMail(builder.buildEmail()).join();
    }

}
