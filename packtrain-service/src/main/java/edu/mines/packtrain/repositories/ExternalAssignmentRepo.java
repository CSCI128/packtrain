package edu.mines.packtrain.repositories;

import edu.mines.packtrain.models.ExternalAssignment;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

public interface ExternalAssignmentRepo extends CrudRepository<ExternalAssignment, UUID>  {
}
