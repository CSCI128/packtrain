package edu.mines.packtrain.seeders;

import edu.mines.packtrain.models.*;
import edu.mines.packtrain.repositories.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;

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
        masterMigration.setMigrations(new ArrayList<>());

        return masterMigrationRepo.save(masterMigration);
    }

    public Migration migration(Assignment assignment, MasterMigration masterMigration){
        Migration migration = new Migration();
        migration.setAssignment(assignment);
        migration.setMasterMigration(masterMigration);

        return migrationRepo.save(migration);
    }


    public void clearAll(){
        migrationRepo.deleteAll();
        masterMigrationRepo.deleteAll();
    }
}