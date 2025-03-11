package edu.mines.gradingadmin.repositories;

import edu.mines.gradingadmin.models.LateRequest;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface LateRequestRepo extends CrudRepository<LateRequest, UUID> {
    LateRequest getLateRequestById(UUID id);
}
