package edu.mines.packtrain.repositories;

import edu.mines.packtrain.models.MigrationTransactionLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface MigrationTransactionLogRepo extends CrudRepository<MigrationTransactionLog, Long> {
    List<MigrationTransactionLog> getAllByMigrationId(UUID migrationId);

    @Query("select l from migration_transaction_log l where l.migrationId = ?1 " +
            "order by l.cwid, l.revision asc")
    List<MigrationTransactionLog> getAllByMigrationIdSorted(UUID migrationId);

    @Query("select l from migration_transaction_log l where l.migrationId = ?2 " +
            "and l.cwid = ?1 order by l.revision asc")
    List<MigrationTransactionLog> getByCwidAndMigrationId(String cwid, UUID migrationId);
}
