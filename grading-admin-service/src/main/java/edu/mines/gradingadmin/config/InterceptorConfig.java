package edu.mines.gradingadmin.config;

import edu.mines.gradingadmin.filters.InstructorCourseFilter;
import edu.mines.gradingadmin.filters.OwnerServicesFilter;
import edu.mines.gradingadmin.filters.StudentCourseFilter;
import edu.mines.gradingadmin.managers.SecurityManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {
    private final SecurityManager securityManager;

    public InterceptorConfig(SecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new InstructorCourseFilter(securityManager))
                .addPathPatterns("/api/instructor/**");
        registry.addInterceptor(new OwnerServicesFilter(securityManager))
                .addPathPatterns("/api/owner/**");
        registry.addInterceptor(new StudentCourseFilter(securityManager))
                .addPathPatterns("/api/student/**");
    }
}
