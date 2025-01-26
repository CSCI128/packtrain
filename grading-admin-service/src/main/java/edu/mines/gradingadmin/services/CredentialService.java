package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.models.Credential;
import edu.mines.gradingadmin.models.ExternalSource;
import edu.mines.gradingadmin.models.User;
import edu.mines.gradingadmin.repositories.CredentialRepo;
import edu.mines.gradingadmin.repositories.ExternalSourceRepo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CredentialService {
    private final CredentialRepo credentialRepo;
    private final ExternalSourceRepo externalSourceRepo;
    private final UserService userService;

    public CredentialService(CredentialRepo credentialRepo, ExternalSourceRepo externalSourceRepo, UserService userService) {
        this.credentialRepo = credentialRepo;
        this.externalSourceRepo = externalSourceRepo;
        this.userService = userService;
    }


    public Optional<String> getCredentialByService(String cwid, String serviceEndpoint){
        List<Credential> availableCredentials = credentialRepo.getByCwidAndEndpoint(cwid, serviceEndpoint);

        if (availableCredentials.isEmpty()){
            return Optional.empty();
        }

        // if there is multiple, return the first

        return Optional.of(availableCredentials.getFirst().getApiKey());
    }

    public Optional<String> getCredentialByService(UUID courseId, String serviceEndpoint){
        List<Credential> availableCredentials = credentialRepo.getByCourseAndEndpoint(courseId, serviceEndpoint);

        if (availableCredentials.isEmpty()){
            return Optional.empty();
        }

        // if there is multiple, return the first

        return Optional.of(availableCredentials.getFirst().getApiKey());
    }

    public Optional<Credential> createNewCredentialForService(String cwid, String name, String apiKey, String serviceEndpoint){
        // todo this needs error handling
        if (credentialRepo.existsByCwidAndEndpoint(cwid, serviceEndpoint)){
            return Optional.empty();
        }

        if (credentialRepo.existsByCwidAndName(cwid, name)){
            return Optional.empty();
        }

        Credential credential = new Credential();

        Optional<User> user = userService.getUserByCwid(cwid);
        Optional<ExternalSource> externalSource = externalSourceRepo.getByEndpoint(serviceEndpoint);

        if (user.isEmpty() || externalSource.isEmpty()){
            // todo: need error handling via error advice handler
            return Optional.empty();
        }

        credential.setOwningUser(user.get());
        credential.setExternalSource(externalSource.get());
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
            return Optional.empty();
        }

        credential.get().setPrivate(false);

        return Optional.of(credentialRepo.save(credential.get()));
    }








}
