package edu.mines.gradingadmin.repositories;

import edu.mines.gradingadmin.models.Course;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CourseRepo extends CrudRepository<Course, UUID> {
}
