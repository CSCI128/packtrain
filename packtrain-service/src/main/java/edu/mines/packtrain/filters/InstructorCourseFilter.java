package edu.mines.packtrain.filters;

import edu.mines.packtrain.managers.SecurityManager;
import edu.mines.packtrain.models.enums.CourseRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

@Slf4j
public class InstructorCourseFilter implements HandlerInterceptor {
    private final SecurityManager securityManager;

    public InstructorCourseFilter(SecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        final Map<String, String> pathVariables = (Map<String, String>) request
                .getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

        if (!pathVariables.containsKey("course_id")) {
            return true;
        }

        log.debug("Validating instructor access to course for user '{}'",
                securityManager.getUser().getEmail());
        String courseId = pathVariables.get("course_id");

        if (courseId.isEmpty()) {
            log.error("Failed to get courseId from protected request!");
            throw new AccessDeniedException("Failed to get course id from request");
        }

        if (courseId.equals("undefined")) {
            throw new AccessDeniedException("Not enrolled in course as an instructor!");
        }

        if (!securityManager.hasCourseMembership(CourseRole.INSTRUCTOR, UUID.fromString(courseId))
                && !securityManager.hasCourseMembership(CourseRole.OWNER,
                UUID.fromString(courseId))) {
            log.warn("Blocked attempt by '{}' to access course '{}' as an instructor!",
                    securityManager.getUser().getEmail(), courseId);
            throw new AccessDeniedException("Not enrolled in course as an instructor!");
        }

        return true;
    }
}
