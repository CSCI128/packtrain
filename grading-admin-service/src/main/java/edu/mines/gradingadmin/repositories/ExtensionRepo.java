package edu.mines.gradingadmin.repositories;

import edu.mines.gradingadmin.models.Extension;
import edu.mines.gradingadmin.models.LateRequest;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;
import java.util.List;

public interface ExtensionRepo extends CrudRepository<Extension, UUID> {
    @Query("select l from late_request l where l.user.cwid like concat('%',?1,'%')")
    List<LateRequest> getAllLateRequestsForStudent(String cwid);

    @Query("select e from extension e where e.id = ?1")
    List<Extension> getAllExtensionsForStudent();

    @Query("select e from extension e where e.id = ?1")
    List<Extension> getExtensionsByMigrationId(UUID migrationId);
}
