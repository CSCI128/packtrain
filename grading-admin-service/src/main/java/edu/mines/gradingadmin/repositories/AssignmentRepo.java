package edu.mines.gradingadmin.repositories;

import edu.mines.gradingadmin.models.Assignment;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface AssignmentRepo extends CrudRepository<Assignment, UUID> {

}
