package edu.mines.gradingadmin.controllers;

import edu.mines.gradingadmin.api.UserApiDelegate;
import edu.mines.gradingadmin.data.CourseDTO;
import edu.mines.gradingadmin.data.CredentialDTO;
import edu.mines.gradingadmin.data.UserDTO;
import edu.mines.gradingadmin.managers.SecurityManager;
import edu.mines.gradingadmin.models.Course;
import edu.mines.gradingadmin.models.Credential;
import edu.mines.gradingadmin.models.CredentialType;
import edu.mines.gradingadmin.models.User;
import edu.mines.gradingadmin.services.CredentialService;
import edu.mines.gradingadmin.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

        return ResponseEntity.ok(new UserDTO()
                .cwid(user.getCwid())
                .email(user.getEmail())
                .name(user.getName())
                .admin(user.isAdmin())
                .enabled(user.isEnabled())
        );
    }

    @Override
    public ResponseEntity<UserDTO> updateUser(UserDTO userDTO) {
        Optional<User> user = userService.updateUser(userDTO.getCwid(), userDTO.getName(), userDTO.getEmail());

        if (user.isEmpty()){
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.accepted().body(user
                .map(u -> new UserDTO()
                        .cwid(user.get().getCwid())
                        .email(user.get().getEmail())
                        .name(user.get().getName())
                        .admin(user.get().isAdmin())
                        .enabled(user.get().isEnabled()))
                .get());
    }

    @Override
    public ResponseEntity<CredentialDTO> newCredential(CredentialDTO credential) {
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
        credentialRes.setPrivate(newCredential.get().isPrivate());

        return ResponseEntity.ok(credentialRes);
    }

    @Override
    public ResponseEntity<List<CredentialDTO>> getCredentials() {
        User user = securityManager.getUser();
        return ResponseEntity.ok(credentialService.getAllCredentials(user.getCwid()).stream()
                .map(cred -> new CredentialDTO()
                        .id(cred.getId().toString())
                        .name(cred.getName())
                        .service(CredentialDTO.ServiceEnum.valueOf(cred.getType().toString()))
                        ._private(cred.isPrivate()))
                .toList()
        );
    }

    @Override
    public ResponseEntity<Void> markCredentialAsPrivate(String credentialId){
        Optional<Credential> credential = credentialService.markCredentialAsPrivate(UUID.fromString(credentialId));
        if (credential.isEmpty()){
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.accepted().build();
    }

    @Override
    public ResponseEntity<Void> markCredentialAsPublic(String credentialId){
        Optional<Credential> credential = credentialService.markCredentialAsPublic(UUID.fromString(credentialId));
        if (credential.isEmpty()){
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.accepted().build();
    }

    @Override
    public ResponseEntity<Void> deleteCredential(String credentialId){
        Optional<Credential> credential = credentialService.getCredentialById(UUID.fromString(credentialId));
        if (credential.isEmpty()){
            return ResponseEntity.notFound().build();
        }
        credentialService.deleteCredential(credential.get());
        return ResponseEntity.noContent().build();
    }
}
