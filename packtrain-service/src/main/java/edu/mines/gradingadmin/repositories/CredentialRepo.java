package edu.mines.gradingadmin.repositories;

import edu.mines.gradingadmin.models.Credential;
import edu.mines.gradingadmin.models.enums.CredentialType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CredentialRepo extends CrudRepository<Credential, UUID> {
    @Query("select c from credential c where c.owningUser.cwid = ?1 and c.id = ?2")
    Optional<Credential> getById(String owningUserCwid, UUID id);

    @Query("select c from credential c where c.owningUser.cwid = ?1 and c.type = ?2 and c.isPrivate = true")
    Optional<Credential> getByCwidAndType(String owningUserCwid, CredentialType type);

    @Query("select c from credential c join course_credential cc on cc.credential.id=c.id where c.isPrivate = false and cc.course.id = ?1 and c.type = ?2")
    List<Credential> getByCourseAndType(UUID courseId, CredentialType type);

    @Query("select c from credential c where c.owningUser.cwid = ?1")
    List<Credential> getByCwid(String owningUserCwid);

    @Query("select c from credential c where c.owningUser.cwid = ?1 and c.name = ?2")
    List<Credential> getByCwidAndName(String cwid, String name);

    @Query("select count(c) > 0 from credential c where c.owningUser.cwid = ?1 and c.type = ?2")
    boolean existsByCwidAndType(String owningUserCwid, CredentialType type);

    @Query("select count(c) > 0 from credential c where c.owningUser.cwid = ?1 and c.name = ?2")
    boolean existsByCwidAndName(String owningUserCwid, String name);
}
