package edu.mines.packtrain.services;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.mines.packtrain.models.Course;
import edu.mines.packtrain.models.LateRequest;
import edu.mines.packtrain.models.User;
import edu.mines.packtrain.data.templates.ExtensionCreatedStudentDTO;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ExtensionEmailService {
    private final EmailService emailService;
    private final Template extensionCreatedStudentTemplate;
    private ObjectMapper mapper;

    public ExtensionEmailService(EmailService emailService,
            @Qualifier("extensionCreatedStudentTemplate") Template extensionCreatedStudentTemplate,
            ObjectMapper mapper
            ) {
        this.emailService = emailService;
        this.extensionCreatedStudentTemplate = extensionCreatedStudentTemplate;
        this.mapper = mapper;
    }

    public void handleExtensionCreated(LateRequest lateRequest, Course course, User requester, User instructor) {
        {
            ExtensionCreatedStudentDTO model = new ExtensionCreatedStudentDTO();
            model.setCourseName(course.getName());
            model.setAssignmentName(lateRequest.getAssignment().getName());
            model.setRequester(requester.getName());
            model.setExtensionDays(lateRequest.getDaysRequested());
            model.setInstructor(instructor.getName());
            model.setExtension(lateRequest.getExtension() != null);

            createExtensionCreatedStudentEmail(requester.getEmail(), model);
        }
    }

    private void createExtensionCreatedStudentEmail(String emailAddress, ExtensionCreatedStudentDTO model) {

        StringWriter writer = new StringWriter();

        try {
            extensionCreatedStudentTemplate.process(mapper.convertValue(model, new TypeReference<Map<String, Object>>(){}), writer);
        } catch (TemplateException | IOException e) {
            log.error("Failed to render email template!", e);
            return;
        }

        String renderedTemplate = writer.toString();

        if (renderedTemplate.isEmpty()) {
            log.error("Failed to render email template! Template is empty!");
            return;
        }

        emailService.sendEmail(emailAddress, List.of(),
                String.format("[Packtrain] [%s] [%s] New Extension Request Created", model.getCourseName(),
                        model.getAssignmentName()),
                renderedTemplate);
    }

}
