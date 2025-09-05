package edu.mines.packtrain.repositories;

import edu.mines.packtrain.models.MasterMigration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface MasterMigrationRepo extends CrudRepository<MasterMigration, UUID> {
    @Query("select a from master_migration a where a.course.id=?1")
    List<MasterMigration> getMasterMigrationsByCourseId(UUID courseId);

    Optional<MasterMigration> getMasterMigrationById(UUID masterMigrationId);
}
