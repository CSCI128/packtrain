package edu.mines.packtrain.repositories;

import edu.mines.packtrain.models.Extension;
import edu.mines.packtrain.models.LateRequest;
import edu.mines.packtrain.models.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;
import java.util.List;

public interface ExtensionRepo extends CrudRepository<Extension, UUID> {
    @Query("select l from late_request l where l.assignment.course.id = ?1 and l.requestingUser = ?2")
    List<LateRequest> getAllLateRequestsForStudent(UUID courseId, User user);

    @Query("select l from late_request l where l.migration.masterMigration.id = ?1")
    List<Extension> getExtensionsByMigrationId(UUID migrationId);
}
