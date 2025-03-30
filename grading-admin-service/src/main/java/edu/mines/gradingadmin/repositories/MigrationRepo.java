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

    @Query("select m from migration m join fetch assignment a on m.assignment.id = a.id join fetch policy p on m.policy.id = p.id where m.masterMigration.id=?1")
    List<Migration> getMigrationListByMasterMigrationId(UUID masterMigrationId);


    @Query("select m from migration m join fetch assignment a on m.assignment.id = a.id join fetch policy p on m.policy.id = p.id where m.id=?1")
    Migration getMigrationById(UUID migrationId);
}
