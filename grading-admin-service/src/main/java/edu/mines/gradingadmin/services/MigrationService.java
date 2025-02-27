package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.models.Assignment;
import edu.mines.gradingadmin.models.Migration;
import edu.mines.gradingadmin.repositories.MigrationRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class MigrationService {
    private final MigrationRepo migrationRepo;

    public MigrationService(MigrationRepo migrationRepo){
        this.migrationRepo = migrationRepo;

    }

    public List<Migration> getAllMigrations(String courseId){
       return migrationRepo.getMigrationsByCourseId(UUID.fromString(courseId));

    }
}
