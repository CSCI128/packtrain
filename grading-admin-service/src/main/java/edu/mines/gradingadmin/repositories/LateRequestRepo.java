package edu.mines.gradingadmin.repositories;

import edu.mines.gradingadmin.models.LateRequest;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface LateRequestRepo extends CrudRepository<LateRequest, UUID> {
    LateRequest getLateRequestById(UUID id);

    @Query("select l from late_request l where l.requestingUser.cwid = ?1 and l.assignment.id = ?2")
    Optional<LateRequest> getByCwidAndAssignment(String cwid, UUID assignmentId);
}
