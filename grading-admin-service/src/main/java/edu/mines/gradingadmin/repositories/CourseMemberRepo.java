package edu.mines.gradingadmin.repositories;

import edu.mines.gradingadmin.models.CourseMember;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CourseMemberRepo extends CrudRepository<CourseMember, UUID> {
}
