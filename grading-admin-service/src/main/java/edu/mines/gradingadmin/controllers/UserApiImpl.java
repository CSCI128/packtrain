package edu.mines.gradingadmin.controllers;

import edu.mines.gradingadmin.api.UserApiDelegate;
import edu.mines.gradingadmin.models.Credential;
import edu.mines.gradingadmin.models.User;
import edu.mines.gradingadmin.services.CredentialService;
import edu.mines.gradingadmin.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

@Controller
public class UserApiImpl implements UserApiDelegate {
    private final UserService userService;
    private final CredentialService credentialService;

    public UserApiImpl(UserService userService, CredentialService credentialService) {
        this.userService = userService;
        this.credentialService = credentialService;
    }

    @Override
    public ResponseEntity<edu.mines.gradingadmin.data.Credential>
    newCredential(String userId, edu.mines.gradingadmin.data.Credential credential) {
        //todo - this needs a user context
        Optional<User> user = userService.getUserByID(userId);

        if (user.isEmpty()) {
            // need to do this with error controller
            return ResponseEntity.notFound().build();
        }

        Optional<Credential> newCredential = credentialService.createNewCredentialForService(
                user.get().getCwid(), credential.getName(),
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
