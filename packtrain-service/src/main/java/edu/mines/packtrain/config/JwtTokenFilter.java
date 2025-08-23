package edu.mines.packtrain.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

//@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenFilter extends OncePerRequestFilter {

//    private final JwtTokenUtil jwtTokenUtil;
//    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        final String authorizationHeader = request.getHeader("Authorization");

        log.info("Authorization header: {}", authorizationHeader);

        String jwtToken = null;
        String username = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwtToken = authorizationHeader.substring(7);
//            username = jwtTokenUtil.getUsername(jwtToken);
            username = "jeff";
        }
        else {
            System.out.println("CHECKS");
            System.out.println(authorizationHeader != null);
//            System.out.println(authorizationHeader.startsWith("Bearer "));
//            System.out.println(s"authorization header was NULL:" + authorizationHeader);
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            log.info("Security context was null, so authorizing user");
            log.info("User details request received for user: {}", username);
//            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

//            if (jwtTokenUtil.isTokenValid(jwtToken, userDetails)) {
            UsernamePasswordAuthenticationToken user = new UsernamePasswordAuthenticationToken(
                    "testUser",                // principal (spoofed username)
                    null,                      // credentials (null since no password check)
                    Collections.singleton(new SimpleGrantedAuthority("ROLE_TEST"))); // spoofed role
                user.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(user);
//            }
        }
        else {
            System.out.println("CHECKS2");
            System.out.println(username);
            System.out.println(SecurityContextHolder.getContext().getAuthentication());
        }


        filterChain.doFilter(request, response);
    }
}
