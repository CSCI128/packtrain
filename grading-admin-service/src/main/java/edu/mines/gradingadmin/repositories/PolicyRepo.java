package edu.mines.gradingadmin.repositories;

import edu.mines.gradingadmin.models.Course;
import edu.mines.gradingadmin.models.Policy;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PolicyRepo extends CrudRepository<Policy, UUID> {
    boolean existsByPolicyURI(String policyURI);

    List<Policy> getPoliciesByCourse(Course course);

    @Query("select c.policyURI from policy c where c.policyURI=?1")
    Optional<Policy> getPolicyByURI(URI policyURI);
}
