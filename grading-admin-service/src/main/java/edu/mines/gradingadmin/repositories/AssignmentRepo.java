package edu.mines.gradingadmin.repositories;

import edu.mines.gradingadmin.models.Assignment;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AssignmentRepo extends CrudRepository<Assignment, UUID> {

}
