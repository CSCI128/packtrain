package edu.mines.gradingadmin.repositories;

import edu.mines.gradingadmin.models.Extension;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;
import java.util.List;

public interface ExtensionRepo extends CrudRepository<Extension, UUID> {
//    @Query("select e from extension e where e.user.id = ?1")
    @Query("select e from extension e")
    List<Extension> getAllExtensionsForStudent(UUID studentId);

    @Query("select e from extension e where e.id = ?1")
    List<Extension> getExtensionsByMigrationId(UUID migrationId);
}
