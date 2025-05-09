package edu.mines.gradingadmin.repositories;

import edu.mines.gradingadmin.models.MigrationTransactionLog;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MigrationTransactionLogRepo extends CrudRepository< MigrationTransactionLog, Long> {
    List<MigrationTransactionLog> getAllByMigrationId(UUID migrationId);

    @Query("select l from migration_transaction_log l where l.migrationId = ?1 order by l.cwid, l.revision desc")
    List<MigrationTransactionLog> getAllByMigrationIdSorted(UUID migrationId);

    Optional<MigrationTransactionLog> getByCwidAndMigrationId(String cwid, UUID migrationId);
}
