package edu.mines.gradingadmin.repositories;

import edu.mines.gradingadmin.models.MigrationTransactionLog;
import org.springframework.data.repository.CrudRepository;

public interface MigrationTransactionLogRepo extends CrudRepository< MigrationTransactionLog, Long> {
}
