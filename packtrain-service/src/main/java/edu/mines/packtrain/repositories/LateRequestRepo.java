package edu.mines.packtrain.repositories;

import edu.mines.packtrain.models.LateRequest;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

public interface LateRequestRepo extends CrudRepository<LateRequest, UUID> {
    LateRequest getLateRequestById(UUID id);

    @Query("select l from late_request l where l.assignment.course.id = ?1")
    List<LateRequest> getAllLateRequests(UUID courseId);

    // we are doing a join fetch here so that we dont need to add the transactional stuff
    @Query("select l from late_request l join fetch user u on u.cwid = l.requestingUser.cwid where l.assignment.id=?1")
    List<LateRequest> getLateRequestsForAssignment(UUID assignmentId);

}
