package edu.mines.gradingadmin.repositories;

import edu.mines.gradingadmin.models.ScheduledTaskDef;
import edu.mines.gradingadmin.models.UserImportTaskDef;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

@Transactional
@Repository
public interface UserImportTaskRepo extends ScheduledTaskRepo<UserImportTaskDef> {
}