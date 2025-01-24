package edu.mines.gradingadmin.seeders;

import edu.mines.gradingadmin.models.ExternalSource;
import edu.mines.gradingadmin.repositories.ExternalSourceRepo;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
//@Profile("test")
public class ExternalSourceSeeders {
    private final ExternalSourceRepo repo;


    public ExternalSourceSeeders(ExternalSourceRepo repo) {
        this.repo = repo;
    }

    public ExternalSource externalSource1(){
        ExternalSource externalSource = new ExternalSource();

        externalSource.setEndpoint("https://test.com/");

        return repo.save(externalSource);
    }

    public void clearAll(){
        repo.deleteAll();
    }
}
