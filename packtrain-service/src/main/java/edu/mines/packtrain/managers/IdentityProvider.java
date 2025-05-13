package edu.mines.packtrain.managers;

import edu.mines.packtrain.models.enums.CredentialType;
import edu.mines.packtrain.models.User;
import org.springframework.security.access.AccessDeniedException;

import java.util.UUID;

public interface IdentityProvider {

    User getUser() throws AccessDeniedException;

    String getCwid() throws AccessDeniedException;

    boolean getIsEnabled() throws AccessDeniedException;

    boolean getIsAdmin() throws AccessDeniedException;

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
    String getCredential(CredentialType type, UUID course);
}
