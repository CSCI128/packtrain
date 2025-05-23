package edu.mines.packtrain.filters;

import edu.mines.packtrain.managers.SecurityManager;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@WebFilter(urlPatterns = "/**")
@Order(1)
public class UserFilter implements Filter {
    private final SecurityManager securityManager;

    public UserFilter(SecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        String path = ((HttpServletRequest) servletRequest).getRequestURI();

        if (path.contains("/api/-/health")){
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        securityManager.setPrincipalFromRequest((HttpServletRequest) servletRequest);
        securityManager.readUserFromRequest();

        if (!securityManager.getIsEnabled()){
            throw new AccessDeniedException("User is not authorized for this service");
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }
}