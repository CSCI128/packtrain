package edu.mines.gradingadmin.repositories;

import edu.mines.gradingadmin.models.RawScore;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RawScoreRepo extends CrudRepository<RawScore, UUID> {
    Optional<RawScore> getByCwid(String cwid);
    Optional<RawScore> getByAssignmentId(UUID assignmentId);

    @Query("select r from raw_score r where r.cwid = ?1 and r.assignmentId = ?2")
    Optional<RawScore> getByCwidAndAssignmentId(String cwid, UUID assignmentId);
}
