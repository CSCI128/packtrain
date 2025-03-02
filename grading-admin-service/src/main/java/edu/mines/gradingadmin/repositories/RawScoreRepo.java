package edu.mines.gradingadmin.repositories;

import edu.mines.gradingadmin.models.RawScore;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface RawScoreRepo extends CrudRepository<RawScore, UUID> {
    Optional<RawScore> getByCwid(String cwid);
    Optional<RawScore> getByAssignmentId(String assignmentId);

    @Query("select r from raw_score r where r.cwid = ?1 and r.assignment_id = ?2")
    Optional<RawScore> getByCwidandAssignmentId(String cwid, UUID assignmentId);
}
