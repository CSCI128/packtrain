package edu.mines.gradingadmin.filters;

import edu.mines.gradingadmin.managers.SecurityManager;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.io.IOException;

@WebFilter(urlPatterns = "/api/admin/**")
@Component
public class AdminServicesFilter implements Filter {
    private final SecurityManager securityManager;

    public AdminServicesFilter(SecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        String path = ((HttpServletRequest) servletRequest).getRequestURI();

        if (!path.startsWith("/api/admin")){
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        if (!securityManager.getIsAdmin()){
            throw new AccessDeniedException("Not authorized");
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }
}
