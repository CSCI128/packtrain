package edu.mines.gradingadmin.repositories;

import edu.mines.gradingadmin.models.Course;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CourseRepo extends CrudRepository<Course, UUID> {

    @Query("select c from course c")
    List<Course> getAll();

    @Query("select c from course c where c.enabled=?1")
    List<Course> getAll(boolean onlyActive);
}
