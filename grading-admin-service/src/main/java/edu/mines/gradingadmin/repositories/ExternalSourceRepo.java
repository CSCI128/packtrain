package edu.mines.gradingadmin.repositories;

import edu.mines.gradingadmin.models.ExternalSource;
import edu.mines.gradingadmin.models.ExternalSourceType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import javax.swing.text.html.Option;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExternalSourceRepo extends CrudRepository<ExternalSource, UUID> {
    boolean existsByEndpoint(String endpoint);

    Optional<ExternalSource> getByEndpoint(String endpoint);

    Optional<ExternalSource> getByType(ExternalSourceType type);

}
