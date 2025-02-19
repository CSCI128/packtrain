package edu.mines.gradingadmin.repositories;

import edu.mines.gradingadmin.models.Course;
import edu.mines.gradingadmin.models.Policy;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PolicyRepo extends CrudRepository<Policy, UUID> {
    boolean existsByPolicyURI(String policyURI);

    List<Policy> getPoliciesByCourse(Course course);
}
