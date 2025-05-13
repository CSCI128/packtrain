package edu.mines.packtrain.managers;

import edu.mines.packtrain.models.enums.CredentialType;
import edu.mines.packtrain.models.User;
import edu.mines.packtrain.services.CredentialService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ImpersonationManager {
    private final CredentialService credentialService;

    public ImpersonationManager(CredentialService credentialService){
        this.credentialService = credentialService;
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




}
