package edu.mines.gradingadmin.repositories;

import edu.mines.gradingadmin.models.Course;
import edu.mines.gradingadmin.models.MasterMigration;
import edu.mines.gradingadmin.models.Migration;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;


import java.util.UUID;
import java.util.List;


@Repository
public interface MigrationRepo  extends CrudRepository<Migration, UUID>{

    @Query("select a from course a where a.id=?1")
    List<Migration> getMigrationsByCourseId(UUID courseId);

    @Query("select m from migration m where m.masterMigration.id=?1")
    List<Migration> getMigrationListByMasterMigrationId(UUID masterMigrationId);


    @Query("select a from migration a where a.id=?1")
    Migration getMigrationById(UUID migrationId);
}
