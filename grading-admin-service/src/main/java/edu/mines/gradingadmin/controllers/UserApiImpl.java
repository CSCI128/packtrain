package edu.mines.gradingadmin.controllers;

import edu.mines.gradingadmin.api.UserApiDelegate;
import edu.mines.gradingadmin.managers.SecurityManager;
import edu.mines.gradingadmin.models.Credential;
import edu.mines.gradingadmin.models.User;
import edu.mines.gradingadmin.services.CredentialService;
import edu.mines.gradingadmin.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

@Controller
public class UserApiImpl implements UserApiDelegate {
    private final UserService userService;
    private final CredentialService credentialService;

    private final SecurityManager securityManager;

    public UserApiImpl(UserService userService, CredentialService credentialService, SecurityManager securityManager) {
        this.userService = userService;
        this.credentialService = credentialService;
        this.securityManager = securityManager;
    }

    @Override
    public ResponseEntity<edu.mines.gradingadmin.data.User> getCurrentUser() {
        User user = securityManager.getUser();
        edu.mines.gradingadmin.data.User userRes = new edu.mines.gradingadmin.data.User();

        userRes.setAdmin(user.isAdmin());
        userRes.setCwid(user.getCwid());
        userRes.setName(user.getName());
        userRes.setEmail(user.getEmail());

        return ResponseEntity.ok(userRes);
    }

    @Override
    public ResponseEntity<edu.mines.gradingadmin.data.Credential>
    newCredential( edu.mines.gradingadmin.data.Credential credential) {
        User user = securityManager.getUser();

        Optional<Credential> newCredential = credentialService.createNewCredentialForService(
                user.getCwid(), credential.getName(),
                credential.getApiKey(), credential.getEndpoint().toString()
        );

        if (newCredential.isEmpty()) {
            // need to do this with error controller
            return ResponseEntity.badRequest().build();
        }
        edu.mines.gradingadmin.data.Credential credentialRes = new edu.mines.gradingadmin.data.Credential();

        try {
            credentialRes.setId(newCredential.get().getId().toString());
            credentialRes.setName(newCredential.get().getName());
            credentialRes.setEndpoint(new URI(newCredential.get().getExternalSource().getEndpoint()));
            credentialRes.setActive(newCredential.get().isActive());
            credentialRes.setPrivate(newCredential.get().isPrivate());
        } catch (URISyntaxException e) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(credentialRes);
    }


}
