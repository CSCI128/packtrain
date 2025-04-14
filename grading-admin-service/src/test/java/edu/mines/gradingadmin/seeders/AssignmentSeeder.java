package edu.mines.gradingadmin.seeders;

import edu.mines.gradingadmin.models.Assignment;
import edu.mines.gradingadmin.models.Course;
import edu.mines.gradingadmin.repositories.AssignmentRepo;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Profile("test")
public class AssignmentSeeder {

    private final AssignmentRepo assignmentRepo;

    public AssignmentSeeder(AssignmentRepo assignmentRepo) {
        this.assignmentRepo = assignmentRepo;
    }

    public Assignment worksheet1(Course owningCourse){
        Assignment assignment = new Assignment();
        assignment.setName("Worksheet 1");
        assignment.setCategory("Worksheets");
        assignment.setCanvasId(92346L);
        assignment.setPoints(10.);
        assignment.setDueDate(Instant.parse("2020-01-30T23:59:59Z"));
        assignment.setUnlockDate(Instant.parse("2020-01-20T00:00:01Z"));
        assignment.setCourse(owningCourse);

        return assignmentRepo.save(assignment);
    }

    public Assignment reading(Course owningCourse){
        Assignment assignment = new Assignment();
        assignment.setName("Week 6 Readings");
        assignment.setCategory("Readings");
        assignment.setCanvasId(92349L);
        assignment.setPoints(24.0);
        assignment.setDueDate(Instant.parse("2020-01-30T23:59:59Z"));
        assignment.setUnlockDate(Instant.parse("2020-01-20T00:00:01Z"));
        assignment.setCourse(owningCourse);

        return assignmentRepo.save(assignment);
    }


    public void clearAll(){
        assignmentRepo.deleteAll();
    }
}
