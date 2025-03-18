package edu.mines.gradingadmin.repositories;

import edu.mines.gradingadmin.models.MigrationTransactionLog;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

public interface MigrationTransactionLogRepo extends CrudRepository< MigrationTransactionLog, Long> {
    List<MigrationTransactionLog> getAllByMigrationId(UUID migrationId);
}
