package edu.mines.gradingadmin.controllers;

import edu.mines.gradingadmin.api.UserApiDelegate;
import edu.mines.gradingadmin.data.CredentialDTO;
import edu.mines.gradingadmin.data.UserDTO;
import edu.mines.gradingadmin.managers.SecurityManager;
import edu.mines.gradingadmin.models.Credential;
import edu.mines.gradingadmin.models.CredentialType;
import edu.mines.gradingadmin.models.User;
import edu.mines.gradingadmin.services.CredentialService;
import edu.mines.gradingadmin.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

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
    public ResponseEntity<UserDTO> getUser() {
        User user = securityManager.getUser();
        UserDTO userRes = new UserDTO();

        userRes.setAdmin(user.isAdmin());
        userRes.setCwid(user.getCwid());
        userRes.setName(user.getName());
        userRes.setEmail(user.getEmail());

        return ResponseEntity.ok(userRes);
    }

    @Override
    public ResponseEntity<CredentialDTO>
    newCredential( CredentialDTO credential) {
        User user = securityManager.getUser();

        Optional<Credential> newCredential = credentialService.createNewCredentialForService(
                user.getCwid(), credential.getName(),
                credential.getApiKey(), CredentialType.fromString(credential.getService().toString())
        );

        if (newCredential.isEmpty()) {
            // need to do this with error controller
            return ResponseEntity.badRequest().build();
        }
        CredentialDTO credentialRes = new CredentialDTO();
        credentialRes.setId(newCredential.get().getId().toString());
        credentialRes.setName(newCredential.get().getName());
        credentialRes.setService(CredentialDTO.ServiceEnum.valueOf(newCredential.get().getType().toString()));
        credentialRes.setActive(newCredential.get().isActive());
        credentialRes.setPrivate(newCredential.get().isPrivate());

        return ResponseEntity.ok(credentialRes);
    }


}
