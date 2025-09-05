package edu.mines.packtrain.repositories;

import edu.mines.packtrain.models.Migration;
import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MigrationRepo extends CrudRepository<Migration, UUID> {
    @Query("select m from migration m where m.masterMigration.id=?1")
    List<Migration> getMigrationListByMasterMigrationId(UUID masterMigrationId);

    @Query("select m from migration m where m.id=?1")
    Migration getMigrationById(UUID migrationId);
}
