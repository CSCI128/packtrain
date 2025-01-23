package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.models.Credential;
import edu.mines.gradingadmin.repositories.CredentialRepo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CredentialService {
    private final CredentialRepo credentialRepo;

    public CredentialService(CredentialRepo credentialRepo) {
        this.credentialRepo = credentialRepo;
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

}
