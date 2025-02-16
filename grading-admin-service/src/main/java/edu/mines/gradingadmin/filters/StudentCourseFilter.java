package edu.mines.gradingadmin.filters;

import edu.mines.gradingadmin.managers.SecurityManager;
import edu.mines.gradingadmin.models.CourseRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;
import java.util.UUID;

@Slf4j
public class StudentCourseFilter implements HandlerInterceptor {
    private final SecurityManager securityManager;

    public StudentCourseFilter(SecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        final Map<String, String> pathVariables = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

        if (!pathVariables.containsKey("courseId")){
            return true;
        }

        log.debug("Validating student access to course for user '{}'", securityManager.getUser());
        String courseId = pathVariables.get("courseId");

        if (courseId.isEmpty()){
            log.error("Failed to get courseId from protected request!");
            throw new AccessDeniedException("Failed to get course Id from request");
        }

        if (!securityManager.hasCourseMembership(CourseRole.STUDENT, UUID.fromString(courseId))){
            log.warn("Blocked attempt by '{}' to access course '{}' as a student!", securityManager.getUser().getEmail(), courseId);
            throw new AccessDeniedException("Not enrolled in course as a student!");
        }

        return true;
    }
}
