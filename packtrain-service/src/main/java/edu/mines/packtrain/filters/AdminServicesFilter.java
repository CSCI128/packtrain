package edu.mines.packtrain.filters;

import edu.mines.packtrain.managers.SecurityManager;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@WebFilter(urlPatterns = "/api/admin/**")
@Component
public class AdminServicesFilter implements Filter {
    private final SecurityManager securityManager;

    public AdminServicesFilter(SecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain filterChain) throws IOException, ServletException {
        String path = ((HttpServletRequest) servletRequest).getRequestURI();

        if (!path.startsWith("/api/admin")) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        if (!securityManager.getIsAdmin()) {
            throw new AccessDeniedException("Not authorized");
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }
}
