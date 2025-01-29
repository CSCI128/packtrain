package edu.mines.gradingadmin.repositories;

import edu.mines.gradingadmin.models.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepo extends CrudRepository<User, String> {
    Optional<User> getByEmail(String name);
    Optional<User> getByCwid(String cwid);

    boolean existsByCwid(String cwid);

    Optional<User> getByOauthId(UUID uuid);
}
