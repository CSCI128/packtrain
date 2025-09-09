package edu.mines.packtrain.config;

import java.io.File;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
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
    private final Template extensionCreatedStudentTemplate;

    public FreeMarkerConfiguration(
            @Value("${grading-admin.email.templates.template-directory}") String templatePath,
            @Value("${grading-admin.email.templates.extension-created-student}") String extensionCreatedStudent) {
        templateLoader = new ClassTemplateLoader(this.getClass(), templatePath);

        freemarker.template.Configuration configuration = new freemarker.template.Configuration(
                freemarker.template.Configuration.VERSION_2_3_34);

        configuration.setTemplateLoader(templateLoader);

        try {
            extensionCreatedStudentTemplate = configuration.getTemplate(extensionCreatedStudent);
        } catch (TemplateNotFoundException e) {
            log.error("Failed to locate template '{}'", extensionCreatedStudent);
            log.error("Template not found", e);
            throw new RuntimeException(e);
        } catch (MalformedTemplateNameException e) {
            log.error("Bad template name '{}'", extensionCreatedStudent);
            log.error("Bad template name", e);
            throw new RuntimeException(e);
        } catch (ParseException e) {
            log.error("Failed to parse template '{}'", extensionCreatedStudent);
            log.error("Parse error!", e);
            throw new RuntimeException(e);
        } catch (IOException e) {
            log.error("Failed to read template!", e);
            throw new RuntimeException(e);
        }
    }

    @Bean(name = "extensionCreatedStudentTemplate")
    public Template getExtensionCreatedStudentTemplate() {
        return extensionCreatedStudentTemplate;
    }

}
