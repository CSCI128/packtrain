package edu.mines.packtrain.config;

import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateNotFoundException;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class FreeMarkerConfiguration {
    private final TemplateLoader templateLoader;
    private final Template extensionCreatedStudentEmailTemplate;
    private final Template extensionApprovedStudentEmailTemplate;
    private final Template extensionDeniedStudentEmailTemplate;
    private final Template extensionCreatedInstructorEmailTemplate;

    public FreeMarkerConfiguration(
            @Value("${grading-admin.email.templates.template-directory}") 
            String templatePath,
            @Value("${grading-admin.email.templates.extension-created-student}") 
            String extensionCreatedStudentTemplateName,
            @Value("${grading-admin.email.templates.extension-approved-student}") 
            String extensionApprovedStudentTemplateName, 
            @Value("${grading-admin.email.templates.extension-denied-student}") 
            String extensionDeniedStudentTemplateName, 
            @Value("${grading-admin.email.templates.extension-created-instructor}") 
            String extensionCreatedInstructorTemplateName
            ) {
        templateLoader = new ClassTemplateLoader(this.getClass(), templatePath);

        freemarker.template.Configuration configuration = new freemarker.template.Configuration(
                freemarker.template.Configuration.VERSION_2_3_34);

        configuration.setTemplateLoader(templateLoader);

        extensionCreatedStudentEmailTemplate = readTemplates(configuration, extensionCreatedStudentTemplateName);
        extensionApprovedStudentEmailTemplate = readTemplates(configuration, extensionApprovedStudentTemplateName);
        extensionDeniedStudentEmailTemplate = readTemplates(configuration, extensionDeniedStudentTemplateName);
        extensionCreatedInstructorEmailTemplate = readTemplates(configuration, extensionCreatedInstructorTemplateName);

    }

    private static Template readTemplates(freemarker.template.Configuration configuration, String template) {
        try {
            return configuration.getTemplate(template);
        } catch (TemplateNotFoundException e) {
            log.error("Failed to locate template '{}'", template);
            log.error("Template not found", e);
            throw new RuntimeException(e);
        } catch (MalformedTemplateNameException e) {
            log.error("Bad template name '{}'", template);
            log.error("Bad template name", e);
            throw new RuntimeException(e);
        } catch (ParseException e) {
            log.error("Failed to parse template '{}'", template);
            log.error("Parse error!", e);
            throw new RuntimeException(e);
        } catch (IOException e) {
            log.error("Failed to read template!", e);
            throw new RuntimeException(e);
        }
    }

    @Bean(name = "extensionCreatedStudentEmailTemplate")
    public Template getExtensionCreatedStudentEmailTemplate() {
        return extensionCreatedStudentEmailTemplate;
    }

    @Bean(name = "extensionApprovedStudentEmailTemplate")
    public Template getExtensionApprovedStudentEmailTemplate() {
        return extensionApprovedStudentEmailTemplate;
    }

    @Bean(name = "extensionDeniedStudentEmailTemplate")
    public Template extensionDeniedStudentEmailTemplate() {
        return extensionDeniedStudentEmailTemplate;
    }

    @Bean(name = "extensionCreatedInstructorEmailTemplate")
    public Template getExtensionCreatedInstructorEmailTemplate() {
        return extensionCreatedInstructorEmailTemplate;
    }
}
