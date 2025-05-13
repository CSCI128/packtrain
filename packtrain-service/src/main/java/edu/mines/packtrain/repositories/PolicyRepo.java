package edu.mines.packtrain.repositories;

import edu.mines.packtrain.models.Course;
import edu.mines.packtrain.models.Policy;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PolicyRepo extends CrudRepository<Policy, UUID> {
    boolean existsByPolicyURI(String policyURI);

    List<Policy> getPoliciesByCourse(Course course);

    @Query("select c from policy c where c.policyURI=?1")
    Optional<Policy> getPolicyByURI(String policyURI);

    Optional<Policy> getPolicyById(UUID policyId);
}
