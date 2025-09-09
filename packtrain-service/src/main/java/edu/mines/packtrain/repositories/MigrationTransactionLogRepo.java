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

    @Query("select l from migration_transaction_log l where l.migrationId = ?1" + 
         "and l.revision = (select max(l2.revision) from migration_transaction_log l2 where l2.cwid = l.cwid )")
    // grabbing the latest revision from migration transaction log by migration id 
    // cursed subquery version (im so sorry)
     List<MigrationTransactionLog> getAllByMigrationIdSorted(UUID migrationId);
}
