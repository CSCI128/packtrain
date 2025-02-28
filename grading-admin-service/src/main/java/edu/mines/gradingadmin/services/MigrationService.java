package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.models.*;
import edu.mines.gradingadmin.repositories.MasterMigrationRepo;
import edu.mines.gradingadmin.repositories.MigrationRepo;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class MigrationService {
    private final MigrationRepo migrationRepo;
    private final MasterMigrationRepo masterMigrationRepo;

    public MigrationService(MigrationRepo migrationRepo, MasterMigrationRepo masterMigrationRepo){
        this.migrationRepo = migrationRepo;
        this.masterMigrationRepo = masterMigrationRepo;

    }

    public MasterMigration createMigrationForAssignment(Course course, List<Policy> policyList, List<Assignment> assignmentList){
        MasterMigration masterMigration = new MasterMigration();
        masterMigration.setCourse(course);
        List<Migration> migrations = new ArrayList<>();
        for (int i = 0; i < assignmentList.size(); i++){
            Migration migration = new Migration();
            migration.setAssignment(assignmentList.get(i));
            migration.setPolicy(policyList.get(i));
            migrations.add(migrationRepo.save(migration));
        }
        masterMigration.setMigrations(migrations);
        return masterMigrationRepo.save(masterMigration);

    }

    @Transactional
    public List<MasterMigration> getAllMasterMigrations(String courseId){
       return masterMigrationRepo.getMasterMigrationsByCourseId(UUID.fromString(courseId));

    }
}
