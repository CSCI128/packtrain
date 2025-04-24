package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.data.CredentialDTO;
import edu.mines.gradingadmin.models.Credential;
import edu.mines.gradingadmin.models.enums.CredentialType;
import edu.mines.gradingadmin.models.User;
import edu.mines.gradingadmin.repositories.CredentialRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class CredentialService {
    private final CredentialRepo credentialRepo;
    private final UserService userService;

    public CredentialService(CredentialRepo credentialRepo, UserService userService) {
        this.credentialRepo = credentialRepo;
        this.userService = userService;
    }

    public List<Credential> getAllCredentials(String cwid){
        Optional<User> user = userService.getUserByCwid(cwid);
        if (user.isEmpty()){
            // todo: also needs error handling via error advice handler
            log.warn("Could not find user with CWID: '{}'", cwid);
            return List.of();
        }

        return credentialRepo.getByCwid(cwid);
    }

    public Optional<Credential> getCredentialById(UUID id) {
        return credentialRepo.getById(id);
    }

    public Optional<String> getCredentialByService(String cwid, CredentialType type){
        List<Credential> availableCredentials = credentialRepo.getByCwidAndEndpoint(cwid, type);

        if (availableCredentials.isEmpty()){
            throw new AccessDeniedException("Access denied: credential does not exist");
        }

        // if there is multiple, return the first

        return Optional.of(availableCredentials.getFirst().getApiKey());
    }

    public Optional<String> getCredentialByService(UUID courseId, CredentialType type){
        List<Credential> availableCredentials = credentialRepo.getByCourseAndEndpoint(courseId, type);

        if (availableCredentials.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Credentials do not exist");
        }

        // if there is multiple, return the first

        return Optional.of(availableCredentials.getFirst().getApiKey());
    }

    public Optional<Credential> createNewCredentialForService(String cwid, CredentialDTO credentialDTO){
        // todo this needs error handling

        Credential credential = new Credential();

        Optional<User> user = userService.getUserByCwid(cwid);

        CredentialType credentialType = CredentialType.fromString(credentialDTO.getService().toString());

        if (user.isEmpty()){
            // todo: need error handling via error advice handler
            log.warn("Could not find user with CWID: '{}'", cwid);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not find a user with this CWID");
        }

        if (credentialRepo.existsByCwidAndEndpoint(cwid, credentialType)){
            log.warn("Credential with service: '{}', already exists", credentialType);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Credential already exists");
        }

        if (credentialRepo.existsByCwidAndName(cwid, credentialDTO.getName())){
            log.warn("Credential with name: '{}', already exists", credentialDTO.getName());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Credential already exists");
        }

        credential.setOwningUser(user.get());
        credential.setType(credentialType);
        credential.setName(credentialDTO.getName());
        credential.setApiKey(credentialDTO.getApiKey());
        credential.setPrivate(true);

        return Optional.of(credentialRepo.save(credential));
    }

    public Optional<Credential> markCredentialAsPublic(UUID credentialId){
        Optional<Credential> credential = credentialRepo.getById(credentialId);

        if (credential.isEmpty()){
            // todo need error handling
            log.warn("Credential with ID: '{}', does not exist", credentialId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Credential does not exist");
        }

        credential.get().setPrivate(false);

        return Optional.of(credentialRepo.save(credential.get()));
    }

    public Optional<Credential> markCredentialAsPrivate(UUID credentialId) {
        Optional<Credential> credential = credentialRepo.getById(credentialId);

        if (credential.isEmpty()){
            // todo need error handling
            log.warn("Credential with ID: '{}', does not exist", credentialId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Credential does not exist");
        }

        credential.get().setPrivate(true);

        return Optional.of(credentialRepo.save(credential.get()));
    }

    public void deleteCredential(Credential credential) {
        credentialRepo.delete(credential);
    }
}
