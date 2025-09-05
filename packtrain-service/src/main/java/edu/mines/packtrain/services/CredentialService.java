package edu.mines.packtrain.services;

import edu.mines.packtrain.data.CredentialDTO;
import edu.mines.packtrain.models.Credential;
import edu.mines.packtrain.models.User;
import edu.mines.packtrain.models.enums.CredentialType;
import edu.mines.packtrain.repositories.CredentialRepo;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class CredentialService {
    private final CredentialRepo credentialRepo;
    private final UserService userService;

    public CredentialService(CredentialRepo credentialRepo, UserService userService) {
        this.credentialRepo = credentialRepo;
        this.userService = userService;
    }

    public List<Credential> getAllCredentials(String cwid) {
        return credentialRepo.getByCwid(cwid);
    }

    public Credential getCredentialById(String cwid, UUID id) {
        Optional<Credential> credential = credentialRepo.getById(cwid, id);

        if (credential.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    String.format("Credential '%s' was not found", id));
        }

        return credential.get();
    }

    public Optional<String> getCredentialByService(String cwid, CredentialType type) {
        return credentialRepo.getByCwidAndType(cwid, type).map(Credential::getApiKey);
    }

    public Optional<String> getCredentialByService(UUID courseId, CredentialType type) {
        List<Credential> availableCredentials = credentialRepo.getByCourseAndType(courseId, type);

        if (availableCredentials.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(availableCredentials.getFirst().getApiKey());
    }

    public Credential createNewCredentialForService(String cwid, CredentialDTO credentialDTO) {
        Credential credential = new Credential();

        User user = userService.getUserByCwid(cwid);

        CredentialType credentialType = CredentialType.fromString(credentialDTO.getService()
                .toString());

        if (credentialRepo.existsByCwidAndType(cwid, credentialType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("Credential type '%s' already exists for user '%s'",
                            credentialType, cwid));
        }

        if (credentialRepo.existsByCwidAndName(cwid, credentialDTO.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("Credential with name '%s' already exists for user '%s'",
                            credentialDTO.getName(), cwid));
        }

        credential.setOwningUser(user);
        credential.setType(credentialType);
        credential.setName(credentialDTO.getName());
        credential.setApiKey(credentialDTO.getApiKey());
        credential.setPrivate(true);

        return credentialRepo.save(credential);
    }

    public Credential markCredentialAsPublic(String cwid, UUID credentialId) {
        Credential credential = getCredentialById(cwid, credentialId);

        credential.setPrivate(false);

        return credentialRepo.save(credential);
    }

    public Credential markCredentialAsPrivate(String cwid, UUID credentialId) {
        Credential credential = getCredentialById(cwid, credentialId);

        credential.setPrivate(true);

        return credentialRepo.save(credential);
    }

    public void deleteCredential(String cwid, UUID credentialId) {
        Credential credential = getCredentialById(cwid, credentialId);

        credentialRepo.delete(credential);
    }
}
