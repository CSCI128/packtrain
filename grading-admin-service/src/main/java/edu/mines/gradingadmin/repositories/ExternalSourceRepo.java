package edu.mines.gradingadmin.repositories;

import edu.mines.gradingadmin.models.ExternalSource;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExternalSourceRepo extends CrudRepository<ExternalSource, UUID> {
    Optional<ExternalSource> getByEndpoint(String endpoint);

}
