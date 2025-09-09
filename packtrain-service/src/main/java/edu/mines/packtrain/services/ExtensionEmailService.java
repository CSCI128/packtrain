package edu.mines.packtrain.services;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.mines.packtrain.data.templates.ExtensionCreatedInstructorDTO;
import edu.mines.packtrain.data.templates.ExtensionCreatedStudentDTO;
import edu.mines.packtrain.models.Course;
import edu.mines.packtrain.models.LateRequest;
import edu.mines.packtrain.models.User;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ExtensionEmailService {
    private final EmailService emailService;
    private final Template extensionCreatedStudentTemplate;
    private final Template extensionCreatedInStructorTemplate;
    private final ObjectMapper mapper;
    private final String frontendUrl;

    public ExtensionEmailService(EmailService emailService,
            @Qualifier("extensionCreatedStudentTemplate") Template extensionCreatedStudentTemplate,
            @Qualifier("extensionCreatedInstructorTemplate") Template extensionCreatedInStructorTemplate,
            @Value("${grading-admin.frontend-url}") String frontendUrl,

            ObjectMapper mapper) {
        this.emailService = emailService;
        this.extensionCreatedStudentTemplate = extensionCreatedStudentTemplate;
        this.extensionCreatedInStructorTemplate = extensionCreatedInStructorTemplate;
        this.frontendUrl = frontendUrl;
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

        if (lateRequest.getExtension() == null) {
            return;
        }

        {
            ExtensionCreatedInstructorDTO model = new ExtensionCreatedInstructorDTO();
            model.setCourseName(lateRequest.getAssignment().getName());
            model.setAssignmentName(course.getName());
            model.setStudent(requester.getName());
            model.setInstructor(instructor.getName());
            model.setExtensionDays(lateRequest.getDaysRequested());
            model.setExplanation(lateRequest.getExtension().getComments());
            model.setPacktrainURL(frontendUrl);

            createExtensionCreatedInstructorEmail(instructor.getEmail(), model);
        }
    }

    private void createExtensionCreatedStudentEmail(String emailAddress, ExtensionCreatedStudentDTO model) {

        StringWriter writer = new StringWriter();

        try {
            extensionCreatedStudentTemplate
                    .process(mapper.convertValue(model, new TypeReference<Map<String, Object>>() {
                    }), writer);
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

    private void createExtensionCreatedInstructorEmail(String emailAddress, ExtensionCreatedInstructorDTO model) {
        StringWriter writer = new StringWriter();

        try {
            extensionCreatedInStructorTemplate
                    .process(mapper.convertValue(model, new TypeReference<Map<String, Object>>() {
                    }), writer);
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
                String.format("[Packtrain] [%s] [%s] [%s] New Extension Request Created", model.getCourseName(),
                        model.getAssignmentName(), model.getStudent()),
                renderedTemplate);
    }

}
