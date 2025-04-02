package edu.mines.gradingadmin.seeders;

import edu.mines.gradingadmin.models.*;
import edu.mines.gradingadmin.repositories.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Profile("test")
public class MigrationSeeder {
    private final MasterMigrationRepo masterMigrationRepo;
    private final MigrationRepo migrationRepo;

    public MigrationSeeder(MasterMigrationRepo masterMigrationRepo, MigrationRepo migrationRepo) {
        this.masterMigrationRepo = masterMigrationRepo;
        this.migrationRepo = migrationRepo;
    }

    public MasterMigration masterMigration(Course owningCourse, User creatingUser){
        MasterMigration masterMigration = new MasterMigration();
        masterMigration.setDateStarted(Instant.now());
        masterMigration.setCreatedByUser(creatingUser);
        masterMigration.setCourse(owningCourse);

        return masterMigrationRepo.save(masterMigration);
    }

    public Migration migration(Assignment assignment, MasterMigration masterMigration){
        Migration migration = new Migration();
        migration.setAssignment(assignment);
        migration.setMasterMigration(masterMigration);

        return migrationRepo.save(migration);
    }


    public void cleanup(){
        migrationRepo.deleteAll();
        masterMigrationRepo.deleteAll();
    }






}
