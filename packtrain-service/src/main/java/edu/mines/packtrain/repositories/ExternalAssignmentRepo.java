package edu.mines.packtrain.repositories;

import edu.mines.packtrain.models.ExternalAssignment;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface ExternalAssignmentRepo extends CrudRepository<ExternalAssignment, UUID>  {
}
