package edu.mines.packtrain.managers;

import edu.mines.packtrain.models.enums.CourseRole;
import edu.mines.packtrain.models.enums.CredentialType;
import edu.mines.packtrain.models.User;
import edu.mines.packtrain.services.CourseMemberService;
import edu.mines.packtrain.services.CredentialService;
import edu.mines.packtrain.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import java.util.Optional;
import java.util.UUID;

@Service
@RequestScope
@Slf4j
@Transactional
public class SecurityManager implements IdentityProvider{
    private final UserService userService;
    private final CredentialService credentialService;
    private final CourseMemberService membershipService;
    private Optional<JwtAuthenticationToken> principal;

    @Getter
    private User user;

    public SecurityManager(UserService userService, CredentialService credentialService, CourseMemberService membershipService) {
        this.userService = userService;
        this.credentialService = credentialService;
        this.membershipService = membershipService;
        principal = Optional.empty();

        user = null;
    }

    public void setPrincipalFromRequest(HttpServletRequest request){
        if (request.getUserPrincipal() instanceof JwtAuthenticationToken token){
            principal = Optional.of(token);
        }

    }

    /**
     * This function reads the user information from the JWT on the request.
     * At a high level, this attempts to look up the user by their oauth user id (the UUID that Authenik assigns them).
     * <br></br>
     * If that fails (the account may have been created via a canvas import or manually via the /admin/users route),
     * then attempt to look up the user by their cwid.
     * <br></br>
     * If that fails, then this is a brand-new user, and we need to create their corresponding account in the db.
     * <br></br>
     * If the user is missing any claims in their JWT, then they will be denied.
     *
     *
     * @throws AccessDeniedException if the user is not allowed to access it for whatever reason
     */
    public void readUserFromRequest() throws AccessDeniedException {
        if (user != null){
            return;
        }

        if (principal.isEmpty()){
            throw new AccessDeniedException("JWT not set");
        }

        final Jwt token = principal.get().getToken();

        String oauthId = token.getSubject();
        String cwid = Optional
                .ofNullable(token.getClaimAsString("cwid"))
                .orElseThrow(() -> new AccessDeniedException("Missing required claim: 'cwid'"));

        user = userService.getUserByOauthId(oauthId)
                .or(() -> attemptToLinkExistingUser(cwid, oauthId))
                .or(() -> attemptToCreateNewUser(token, oauthId, cwid))
                .orElseThrow(() -> new AccessDeniedException("Failed to authorized user"));
    }

    /**
     * This function attempts to link the oauth id to an existing user based on the cwid.
     * <br></br>
     * If the user does not exist, then an empty optional is returned.
     *
     * @param cwid the user's cwid as claimed by the JWT
     * @param oauthId the users oauth ID (the subject field)
     * @return An optional populated with the current user, or an empty optional if the user
     */
    private Optional<User> attemptToLinkExistingUser(String cwid, String oauthId){
        Optional<User> loadedUser = userService.findUserByCwid(cwid);

        if (loadedUser.isPresent()){
            loadedUser = userService.linkCwidToOauthId(loadedUser.get(), oauthId);
        }

        return loadedUser;
    }


    /**
     * This function attempts to create a new user in the database given a JWT and its claims.
     * <br></br>
     * The JWT must have the email claim, the is_admin claim, and the name claim to be created.
     *
     * @param token The JWT token
     * @param oauthId the oauth id to link this newly created user to
     * @param cwid the user's cwid
     * @return a populated optional with the newly created user. It will only be empty
     * @throws AccessDeniedException if we are missing any of the required claims
     */
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


    public String getCwid() throws AccessDeniedException{
        if (user == null){
            throw new AccessDeniedException("No user context set.");
        }

        return user.getCwid();
    }

    public boolean getIsEnabled() throws AccessDeniedException {
        if (user == null){
            throw new AccessDeniedException("No user context set.");
        }

        return user.isEnabled();
    }

    public boolean getIsAdmin() throws AccessDeniedException {
        if (user == null){
            throw new AccessDeniedException("No user context set.");
        }

        return user.isAdmin();
    }

    /**
     * This function gets a credentials that a user is entitled to.
     * <br></br>
     * If the user has set a private credential, then that will be tried first.
     * Then it will defer to the course credential.
     * <br></br>
     * If a user doesn't have a credential set, then they will receive a 403 error.
     *
     * @param type the type of credential to look up
     * @param course the course to look up credentials for if the user doesn't have any set
     * @return the credential
     */
    public String getCredential(CredentialType type, UUID course){
        if (user == null){
            throw new AccessDeniedException("No user context set.");
        }

        log.debug("Getting credential for '{}' for user '{}' in context of course '{}'", type, user.getEmail(), course);

        return credentialService.getCredentialByService(user.getCwid(), type)
                .or(() -> credentialService.getCredentialByService(course, type))
                .orElseThrow(() -> new AccessDeniedException("No valid credentials found!"));

    }

    public boolean hasCourseMembership(CourseRole role, UUID courseId){
        if (user == null){
            throw new AccessDeniedException("No user context set.");
        }

        log.debug("Verifying '{}' has access to course '{}' with role '{}'", user.getEmail(), courseId, role.getRole());

        return membershipService.getRoleForUserAndCourse(user, courseId) == role;
    }
}
