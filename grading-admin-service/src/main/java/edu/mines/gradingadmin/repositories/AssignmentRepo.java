package edu.mines.gradingadmin.repositories;

import edu.mines.gradingadmin.models.Assignment;
import edu.mines.gradingadmin.models.Course;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.List;

@Repository
public interface AssignmentRepo extends CrudRepository<Assignment, UUID> {
    Optional<Assignment> getAssignmentById(UUID id);

    @Query("select a from assignment a where a.course.id=?1 and a.enabled = true")
    List<Assignment> getAssignmentsByCourseId(UUID courseId);

    @Query("select a from assignment a where a.course.id=?1")
    List<Assignment> getAllAssignmentsByCourseId(UUID courseId);

    @Query("select a.canvasId from assignment a where a.course = ?1")
    Set<Long> getAssignmentIdsByCourse(Course course);

    @Query("delete from assignment a where a.course = ?1 and a.canvasId in ?2")
    @Modifying
    void deleteByCourseAndCanvasId(Course course, Set<Long> canvasIds);


}
