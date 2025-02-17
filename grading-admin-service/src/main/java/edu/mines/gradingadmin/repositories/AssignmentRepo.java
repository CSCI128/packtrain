package edu.mines.gradingadmin.repositories;

import edu.mines.gradingadmin.models.Assignment;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface AssignmentRepo extends CrudRepository<Assignment, UUID> {
    Optional<Assignment> getAssignmentById(UUID id);

    @Query("select a from assignment a where a.course.id=?1 and a.enabled = true")
    List<Assignment> getAssignmentsByCourseId(UUID courseId);

    @Query("select a from assignment a where a.course.id=?1")
    List<Assignment> getAllAssignmentsByCourseId(UUID courseId);
}
