package edu.mines.packtrain.services;

import java.util.List;
import org.simplejavamail.api.email.EmailPopulatingBuilder;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.email.EmailBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmailService {
    private final boolean enabled;
    private final Mailer mailer;
    private final String fromName;
    private final String fromEmail;
    private final String overrideCC;
    private final String overrideTo;

    public EmailService(
            Mailer mailer,
            @Value("${grading-admin.email.enabled}") boolean enabled,
            @Value("${grading-admin.email.from-email}") String fromEmail,
            @Value("${grading-admin.email.from-name}") String fromName,
            @Value("${grading-admin.email.testing.override-cc:#{null}}") String overrideCC,
            @Value("${grading-admin.email.testing.override-to:#{null}}") String overrideTo) {

        this.enabled = enabled;
        this.mailer = mailer;
        this.fromName = fromName;
        this.fromEmail = fromEmail;
        this.overrideCC = overrideCC;
        this.overrideTo = overrideTo;

        if (this.overrideCC != null && !this.overrideCC.equals("null")) {
            log.warn("CC has been overridden so all emails will be cc'd to {}", overrideCC);
        }

        if (this.overrideTo != null && !this.overrideCC.equals("null")) {
            log.warn("TO has been overridden so all emails will be sent to {}", overrideTo);
        }
    }

    public void sendEmail(String to, List<String> cc, String subject, String html) {
        if (!enabled) {
            return;
        }

        EmailPopulatingBuilder builder = EmailBuilder.startingBlank()
                .from(fromName, fromEmail)
                .to(overrideTo != null ? overrideTo : to)
                .withSubject(subject)
                .withHTMLText(html);

        (overrideCC != null ? List.of(overrideCC) : cc).forEach(s -> builder.cc(s));

        log.debug("Sending email '{}' to '{}'", subject, to);

        mailer.sendMail(builder.buildEmail());
    }

}
