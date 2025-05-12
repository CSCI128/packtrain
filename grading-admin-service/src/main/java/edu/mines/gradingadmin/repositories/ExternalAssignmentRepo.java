package edu.mines.gradingadmin.repositories;

import edu.mines.gradingadmin.models.ExternalAssignment;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface ExternalAssignmentRepo extends CrudRepository<ExternalAssignment, UUID>  {
}
