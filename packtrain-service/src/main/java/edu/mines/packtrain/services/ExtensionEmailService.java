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
import edu.mines.packtrain.data.templates.ExtensionCreatedInstructorEmailDTO;
import edu.mines.packtrain.data.templates.ExtensionCreatedStudentEmailDTO;
import edu.mines.packtrain.models.Course;
import edu.mines.packtrain.models.LateRequest;
import edu.mines.packtrain.models.User;
import edu.mines.packtrain.models.enums.LateRequestType;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ExtensionEmailService {
    private final EmailService emailService;
    private final Template extensionCreatedStudentEmailTemplate;
    private final Template extensionCreatedInStructorEmailTemplate;
    private final ObjectMapper mapper;
    private final String frontendUrl;

    public ExtensionEmailService(EmailService emailService,
            @Qualifier("extensionCreatedStudentEmailTemplate") Template extensionCreatedStudentEmailTemplate,
            @Qualifier("extensionCreatedInstructorEmailTemplate") Template extensionCreatedInStructorEmailTemplate,
            @Value("${grading-admin.frontend-url}") String frontendUrl,

            ObjectMapper mapper) {
        this.emailService = emailService;
        this.extensionCreatedStudentEmailTemplate = extensionCreatedStudentEmailTemplate;
        this.extensionCreatedInStructorEmailTemplate = extensionCreatedInStructorEmailTemplate;
        this.frontendUrl = frontendUrl;
        this.mapper = mapper;
    }

    public void handleExtensionCreated(LateRequest lateRequest, Course course, User requester, User instructor) {
        {
            ExtensionCreatedStudentEmailDTO model = new ExtensionCreatedStudentEmailDTO();
            model.setCourseName(course.getName());
            model.setAssignmentName(lateRequest.getAssignment().getName());
            model.setRequester(requester.getName());
            model.setExtensionDays(lateRequest.getDaysRequested());
            model.setInstructor(instructor.getName());
            model.setExtension(lateRequest.getLateRequestType() == LateRequestType.EXTENSION);

            createExtensionCreatedStudentEmail(requester.getEmail(), model);
        }

        if (lateRequest.getLateRequestType() == LateRequestType.LATE_PASS) {
            return;
        }


        {
            ExtensionCreatedInstructorEmailDTO model = new ExtensionCreatedInstructorEmailDTO();
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

    private void createExtensionCreatedStudentEmail(String emailAddress, ExtensionCreatedStudentEmailDTO model) {

        StringWriter writer = new StringWriter();

        try {
            extensionCreatedStudentEmailTemplate
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

    private void createExtensionCreatedInstructorEmail(String emailAddress, ExtensionCreatedInstructorEmailDTO model) {
        StringWriter writer = new StringWriter();

        try {
            extensionCreatedInStructorEmailTemplate
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
