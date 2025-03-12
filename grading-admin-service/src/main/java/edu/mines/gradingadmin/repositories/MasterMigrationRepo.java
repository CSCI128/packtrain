package edu.mines.gradingadmin.repositories;

import edu.mines.gradingadmin.models.Course;
import edu.mines.gradingadmin.models.MasterMigration;
import edu.mines.gradingadmin.models.Migration;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

public interface MasterMigrationRepo extends CrudRepository<MasterMigration, UUID>  {
    @Query("select a from master_migration a where a.course.id=?1")
    List<MasterMigration> getMasterMigrationsByCourseId(UUID courseId);

    @Query("select a from master_migration a where a.id=?1")
    MasterMigration getMasterMigrationByMasterMigrationId(UUID masterMigrationId);

    @Query("select m from migration m where m.masterMigration.id=?1")
    List<Migration> getMigrationListByMasterMigrationId(UUID masterMigrationId);


}
