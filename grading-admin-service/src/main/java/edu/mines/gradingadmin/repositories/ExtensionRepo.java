package edu.mines.gradingadmin.repositories;

import edu.mines.gradingadmin.models.Extension;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;
import java.util.List;

public interface ExtensionRepo extends CrudRepository<Extension, UUID> {
    @Query("select a from extension a where a.id=?1")
    List<Extension> getExtensionByMigrationId(UUID migrationId);
}
