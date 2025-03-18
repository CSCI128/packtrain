package edu.mines.gradingadmin.managers;

import edu.mines.gradingadmin.models.CredentialType;
import edu.mines.gradingadmin.models.User;
import edu.mines.gradingadmin.services.CredentialService;
import edu.mines.gradingadmin.services.UserService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.Optional;
import java.util.UUID;

@Service
public class ImpersonationManager {
    private final CredentialService credentialService;
    private final UserService userService;

    public ImpersonationManager(CredentialService credentialService, UserService userService){
        this.credentialService = credentialService;
        this.userService = userService;
    }

    @Slf4j
    public static class ImpersonatedUserProvider implements IdentityProvider {


        private final CredentialService credentialService;
        @Getter
        User user;

        protected ImpersonatedUserProvider(User user, CredentialService credentialService){
            this.user = user;
            this.credentialService = credentialService;
        }

        @Override
        public String getCwid() throws AccessDeniedException {
            return user.getCwid();
        }

        @Override
        public boolean getIsEnabled() throws AccessDeniedException {
            return user.isEnabled();
        }

        @Override
        public boolean getIsAdmin() throws AccessDeniedException {
            return user.isAdmin();
        }

        @Override
        public String getCredential(CredentialType type, UUID course) {
            log.debug("Getting credential for '{}' for user '{}' in context of course '{}'", type, user.getEmail(), course);

            return credentialService.getCredentialByService(user.getCwid(), type)
                    .or(() -> credentialService.getCredentialByService(course, type))
                    .orElseThrow(() -> new AccessDeniedException("No valid credentials found!"));
        }

    }

    public ImpersonatedUserProvider impersonateUser(User user){
        return new ImpersonatedUserProvider(user, credentialService);
    }

    public ImpersonatedUserProvider impersonateUser(Principal principal){
        if (!(principal instanceof JwtAuthenticationToken)){
            throw new AccessDeniedException("Failed to decode JWT");
        }
        final Jwt token = ((JwtAuthenticationToken) principal).getToken();

        String oauthId = token.getSubject();
        String cwid = Optional
                .ofNullable(token.getClaimAsString("cwid"))
                .orElseThrow(() -> new AccessDeniedException("Missing required claim: 'cwid'"));

        User user = userService.getUserByOauthId(oauthId)
                .or(() -> attemptToLinkExistingUser(cwid, oauthId))
                .or(() -> attemptToCreateNewUser(token, oauthId, cwid))
                .orElseThrow(() -> new AccessDeniedException("Failed to authorized user"));

        return new ImpersonatedUserProvider(user, credentialService);
    }

    private Optional<User> attemptToLinkExistingUser(String cwid, String oauthId){
        Optional<User> loadedUser = userService.getUserByCwid(cwid);

        if (loadedUser.isPresent()){
            loadedUser = userService.linkCwidToOauthId(loadedUser.get(), oauthId);
        }

        return loadedUser;
    }


    private Optional<User> attemptToCreateNewUser(final Jwt token, final String oauthId, final String cwid) throws AccessDeniedException {
        String email = Optional
                .ofNullable(token.getClaimAsString("email"))
                .orElseThrow(() -> new AccessDeniedException("Missing required claim: 'email'"));

        String name = Optional
                .ofNullable(token.getClaimAsString("name"))
                .orElseThrow(() -> new AccessDeniedException("Missing required claim: 'name'"));
        boolean isAdmin = Optional
                .ofNullable((Boolean) token.getClaim("is_admin"))
                .orElseThrow(() -> new AccessDeniedException("Missing required claim: 'is_admin'"));

        return userService.createNewUser(cwid, isAdmin, name, email, oauthId);
    }


}
