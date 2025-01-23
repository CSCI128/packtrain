package edu.mines.gradingadmin.repositories;

import edu.mines.gradingadmin.models.Credential;
import edu.mines.gradingadmin.models.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

public interface CredentialRepo extends CrudRepository<Credential, UUID> {
    @Query("select c from credential c where c.owningUser.cwid = ?1 and c.externalSource.endpoint = ?2 and c.is_active = true")
    List<Credential> getByCwidAndEndpoint(String owningUserCwid, String endpoint);

    @Query("select c from credential c join course_credential cc on cc.credential.id=c.id where c.is_private = false and c.is_active = true and cc.is_active = true and cc.course.id = ?1 and c.externalSource.endpoint = ?2")
    List<Credential> getByCourseAndEndpoint(UUID courseId, String endpoint);
}
