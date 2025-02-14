package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.models.Credential;
import edu.mines.gradingadmin.models.CredentialType;
import edu.mines.gradingadmin.models.User;
import edu.mines.gradingadmin.repositories.CredentialRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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

        List<Credential> credentials = credentialRepo.getAllByCwid(cwid);

        return credentials;
    }

    public Optional<String> getCredentialByService(String cwid, CredentialType type){
        List<Credential> availableCredentials = credentialRepo.getByCwidAndEndpoint(cwid, type);

        if (availableCredentials.isEmpty()){
            return Optional.empty();
        }

        // if there is multiple, return the first

        return Optional.of(availableCredentials.getFirst().getApiKey());
    }

    public Optional<String> getCredentialByService(UUID courseId, CredentialType type){
        List<Credential> availableCredentials = credentialRepo.getByCourseAndEndpoint(courseId, type);

        if (availableCredentials.isEmpty()){
            return Optional.empty();
        }

        // if there is multiple, return the first

        return Optional.of(availableCredentials.getFirst().getApiKey());
    }

    public Optional<Credential> createNewCredentialForService(String cwid, String name, String apiKey, CredentialType type){
        // todo this needs error handling

        Credential credential = new Credential();

        Optional<User> user = userService.getUserByCwid(cwid);

        if (user.isEmpty()){
            // todo: need error handling via error advice handler
            log.warn("Could not find user with CWID: '{}'", cwid);
            return Optional.empty();
        }

        if (credentialRepo.existsByCwidAndEndpoint(cwid, type)){
            log.warn("Credential with service: '{}', already exists", type);
            return Optional.empty();
        }

        if (credentialRepo.existsByCwidAndName(cwid, name)){
            log.warn("Credential with name: '{}', already exists", name);
            return Optional.empty();
        }

        credential.setOwningUser(user.get());
        credential.setType(type);
        credential.setName(name);
        credential.setApiKey(apiKey);
        credential.setActive(true);
        credential.setPrivate(true);

        return Optional.of(credentialRepo.save(credential));
    }

    public Optional<Credential> markCredentialAsPublic(UUID credentialId){
        Optional<Credential> credential = credentialRepo.getById(credentialId);

        if (credential.isEmpty()){
            // todo need error handling
            log.warn("Credential with ID: '{}', does not exist", credentialId);
            return Optional.empty();
        }

        if (!credential.get().isActive()){
            log.warn("Credential with ID: '{}', is not active", credentialId);
            return Optional.empty();
        }

        credential.get().setPrivate(false);

        return Optional.of(credentialRepo.save(credential.get()));
    }

    public Optional<Credential> markCredentialAsPrivate(UUID credentialId) {
        Optional<Credential> credential = credentialRepo.getById(credentialId);

        if (credential.isEmpty()){
            // todo need error handling
            log.warn("Credential with ID: '{}', does not exist", credentialId);
            return Optional.empty();
        }

        if (!credential.get().isActive()){
            log.warn("Credential with ID: '{}', is not active", credentialId);
            return Optional.empty();
        }

        credential.get().setPrivate(true);

        return Optional.of(credentialRepo.save(credential.get()));
    }

    public Optional<Credential> markCredentialAsInactive(UUID credentialId) {
        Optional<Credential> credential = credentialRepo.getById(credentialId);

        if (credential.isEmpty()){
            log.warn("Credential with ID: '{}', does not exist", credentialId);
            return Optional.empty();
        }

        credential.get().setActive(false);

        return Optional.of(credentialRepo.save(credential.get()));
    }

}
