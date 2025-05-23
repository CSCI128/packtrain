package edu.mines.packtrain.repositories;

import edu.mines.packtrain.models.CourseMember;
import edu.mines.packtrain.models.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepo extends CrudRepository<User, String> {
    Optional<User> getByEmail(String name);
    Optional<User> getByCwid(String cwid);

    boolean existsByCwid(String cwid);

    @Query("select u from user u where u.oAuthId = ?1")
    Optional<User> getByOAuthId(UUID uuid);

    @Query("select u from user u")
    List<User> getAll();

    @Query("select u.courseMemberships from user u where u.cwid = ?1")
    List<CourseMember> getMembershipsByCwid(String cwid);

}
