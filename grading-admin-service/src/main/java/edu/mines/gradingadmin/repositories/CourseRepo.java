package edu.mines.gradingadmin.repositories;

import edu.mines.gradingadmin.models.Course;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface CourseRepo extends CrudRepository<Course, UUID> {
}
