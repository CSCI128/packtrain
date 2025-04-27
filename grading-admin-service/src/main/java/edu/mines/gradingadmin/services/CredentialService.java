package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.data.CredentialDTO;
import edu.mines.gradingadmin.models.Credential;
import edu.mines.gradingadmin.models.enums.CredentialType;
import edu.mines.gradingadmin.models.User;
import edu.mines.gradingadmin.repositories.CredentialRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
        User user = userService.getUserByCwid(cwid);

        return credentialRepo.getByCwid(cwid);
    }

    public Optional<Credential> getCredentialById(UUID id) {
        return credentialRepo.getById(id);
    }

    public Optional<String> getCredentialByService(String cwid, CredentialType type){
        return credentialRepo.getByCwidAndType(cwid, type).map(Credential::getApiKey);
    }

    public Optional<String> getCredentialByService(UUID courseId, CredentialType type){
        List<Credential> availableCredentials = credentialRepo.getByCourseAndType(courseId, type);

        if (availableCredentials.isEmpty()){
            return Optional.empty();
        }

        return Optional.of(availableCredentials.getFirst().getApiKey());
    }

    public Credential createNewCredentialForService(String cwid, CredentialDTO credentialDTO){
        Credential credential = new Credential();

        User user = userService.getUserByCwid(cwid);

        CredentialType credentialType = CredentialType.fromString(credentialDTO.getService().toString());

        if (credentialRepo.existsByCwidAndType(cwid, credentialType)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("Credential type '%s' already exists for user '%s'", credentialType, cwid));
        }

        if (credentialRepo.existsByCwidAndName(cwid, credentialDTO.getName())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("Credential with name '%s' already exists for user '%s'", credentialDTO.getName(), cwid));
        }

        credential.setOwningUser(user);
        credential.setType(credentialType);
        credential.setName(credentialDTO.getName());
        credential.setApiKey(credentialDTO.getApiKey());
        credential.setPrivate(true);

        return credentialRepo.save(credential);
    }

    public Credential markCredentialAsPublic(UUID credentialId){
        Optional<Credential> credential = credentialRepo.getById(credentialId);

        if (credential.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Credential does not exist");
        }

        credential.get().setPrivate(false);

        return credentialRepo.save(credential.get());
    }

    public Credential markCredentialAsPrivate(UUID credentialId) {
        Optional<Credential> credential = credentialRepo.getById(credentialId);

        if (credential.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Credential does not exist");
        }

        credential.get().setPrivate(true);

        return credentialRepo.save(credential.get());
    }

    public void deleteCredential(UUID credentialId) {
        Optional<Credential> credential = credentialRepo.getById(credentialId);

        if (credential.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Credential does not exist");
        }


        credentialRepo.delete(credential.get());
    }
}
