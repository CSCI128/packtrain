package edu.mines.gradingadmin.managers;

import edu.mines.gradingadmin.models.User;
import edu.mines.gradingadmin.services.UserService;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import javax.swing.text.html.Option;
import java.util.Optional;

@Service
@RequestScope
@Slf4j
public class SecurityManager {
    private final UserService userService;
    private Optional<JwtAuthenticationToken> principal;
    private User user;


    public SecurityManager(UserService userService) {
        this.userService = userService;
        principal = Optional.empty();
    }

    public void setPrincipalFromRequest(HttpServletRequest request){
        if (request.getUserPrincipal() instanceof JwtAuthenticationToken token){
            principal = Optional.of(token);
        }

    }

    public User getUserFromRequest() throws AccessDeniedException {
        if (user != null){
            return user;
        }

        if (principal.isEmpty()){
            throw new AccessDeniedException("JWT not set");
        }

        final Jwt token = principal.get().getToken();

        String oauthId = token.getSubject();
        String cwid = token.getClaimAsString("cwid");

        user = userService.getUserByOauthId(oauthId)
                .or(() -> attemptToLinkExistingUser(cwid, oauthId))
                .or(() -> attemptToCreateNewUser(token, oauthId, cwid))
                .orElseThrow(() -> new AccessDeniedException("Failed to authorized user"));

        return user;
    }

    private Optional<User> attemptToLinkExistingUser(String cwid, String oauthId){
        Optional<User> loadedUser = userService.getUserByCwid(cwid);
        // this is yucky
        if (loadedUser.isPresent()){
            loadedUser = userService.linkCwidToOauthId(loadedUser.get(), oauthId);
        }

        return loadedUser;
    }

    private Optional<User> attemptToCreateNewUser(final Jwt token, final String oauthId, final String cwid) {
        String email = token.getClaimAsString("email");
        String name = token.getClaimAsString("name");
        boolean isAdmin = token.getClaim("is_admin");


        return userService.createNewUser(cwid, isAdmin, name, email, oauthId);
    }


    private String getCwid() throws AccessDeniedException{
        if (user == null){
            throw new AccessDeniedException("No user context set.");
        }

        return user.getCwid();
    }

}
