package edu.mines.gradingadmin.repositories;

import edu.mines.gradingadmin.models.User;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepo extends CrudRepository<User, UUID> {
    Optional<User> getByEmail(String name);
    Optional<User> getByCwid(String cwid);
}
