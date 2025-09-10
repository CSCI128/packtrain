package edu.mines.packtrain.services;

import edu.mines.packtrain.data.UserDTO;
import edu.mines.packtrain.models.Course;
import edu.mines.packtrain.models.CourseMember;
import edu.mines.packtrain.models.User;
import edu.mines.packtrain.models.enums.CourseRole;
import edu.mines.packtrain.repositories.UserRepo;
import jakarta.transaction.Transactional;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Cacheable("users")
@Slf4j
public class UserService {
    private final UserRepo userRepo;

    public UserService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    public Optional<User> findUserByCwid(String cwid) {
        return userRepo.getByCwid(cwid);
    }

    public Optional<User> findUserByEmail(String email) {
        return userRepo.getByEmail(email);
    }

    public User getUserByCwid(String cwid) {
        Optional<User> user = userRepo.getByCwid(cwid);
        if (user.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("User '%s' " +
                    "does not exist!", cwid));
        }

        return user.get();
    }

    public User getUserByEmail(String email) {
        Optional<User> user = userRepo.getByEmail(email);

        if (user.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("User '%s' " +
                    "does not exist!", email));
        }

        return user.get();

    }

    public Optional<User> getUserByOauthId(String id) {
        return userRepo.getByOAuthId(UUID.fromString(id));
    }

    public Optional<User> linkCwidToOauthId(User user, String id) {
        user.setOAuthId(UUID.fromString(id));

        return Optional.of(userRepo.save(user));
    }

    public List<User> getAllUsers() {
        return userRepo.getAll();
    }

    public User updateUser(UserDTO userDTO) {
        User user = getUserByCwid(userDTO.getCwid());

        user.setEmail(userDTO.getEmail());
        user.setName(userDTO.getName());

        return userRepo.save(user);
    }

    public List<User> getOrCreateUsersFromCanvas(Map<String, edu.ksu.canvas.model.User>
                                                         canvasUsers) {
        List<User> users = new LinkedList<>();

        for (edu.ksu.canvas.model.User user : canvasUsers.values()) {
            Optional<User> newUser = findUserByCwid(user.getSisUserId())
                    .or(() -> createNewUser(user.getSisUserId(), false,
                            user.getName(), user.getEmail()));

            if (newUser.isEmpty()) {
                log.warn("Failed to look up or create user {}!", user.getEmail());
                continue;
            }

            users.add(newUser.get());
        }

        if (users.size() != canvasUsers.size()) {
            log.warn("Not all users were created successfully! Expected {} users. Only {} " +
                    "users were created successfully", canvasUsers.size(), users.size());
        }

        log.info("Created {} users", users.size());

        return users;
    }

    public Optional<User> createNewUser(String cwid, boolean isAdmin, String name, String email) {
        if (userRepo.existsByCwid(cwid)) {
            return Optional.empty();
        }

        return createNewUser(cwid, isAdmin, name, email, null);
    }

    public Optional<User> createNewUser(String cwid, boolean isAdmin, String name,
                                        String email, @Nullable String oauthId) {
        if (userRepo.existsByCwid(cwid)) {
            return Optional.empty();
        }

        User user = new User();

        user.setCwid(cwid);
        user.setName(name);
        user.setEmail(email);
        user.setAdmin(isAdmin);
        user.setEnabled(true);

        if (oauthId != null) {
            user.setOAuthId(UUID.fromString(oauthId));
            log.debug("Linked oauth id to user!");
        }

        log.debug("Created new user {}", user);

        return Optional.of(userRepo.save(user));
    }

    public boolean disableUser(User actingUser, String cwidToDisable) {
        if (actingUser.getCwid().equals(cwidToDisable)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to disable self");
        }

        if (!actingUser.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, String.format("Admin " +
                    "action attempted by non admin user '%s'!", actingUser.getEmail()));
        }

        User user = getUserByCwid(cwidToDisable);

        if (user.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("User " +
                    "'%s' is an admin, can not disable an admin user", cwidToDisable));
        }

        user.setEnabled(false);

        return true;
    }

    public boolean enableUser(User actingUser, String cwidToEnable) {
        if (!actingUser.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, String.format("Admin " +
                    "action attempted by non admin user '%s'!", actingUser.getEmail()));
        }

        User user = getUserByCwid(cwidToEnable);

        user.setEnabled(true);

        return true;
    }

    public boolean makeAdmin(User actingUser, String cwidToMakeAdmin) {
        if (!actingUser.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, String.format("Admin " +
                    "action attempted by non admin user '%s'!", actingUser.getEmail()));
        }

        User user = getUserByCwid(cwidToMakeAdmin);

        boolean isStudent = getCourseMemberships(cwidToMakeAdmin).stream()
                .anyMatch(m -> m.getRole() == CourseRole.STUDENT);

        if (isStudent) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("User " +
                    "'%s' is a student in at least one course, can not make student an admin " +
                    "user", cwidToMakeAdmin));
        }

        user.setAdmin(true);

        userRepo.save(user);

        log.info("Made user '{}' an admin", user.getEmail());

        return true;
    }

    public boolean demoteAdmin(User actingUser, String cwid) {
        if (!actingUser.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, String.format("Admin " +
                    "action attempted by non admin user '%s'!", actingUser.getEmail()));
        }

        if (actingUser.getCwid().equals(cwid)) {
            log.warn("Attempt to demote current user '{}' from admin!", cwid);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to demote self!");
        }
        User user = getUserByCwid(cwid);

        user.setAdmin(false);

        userRepo.save(user);

        log.info("Demoted admin '{}' to user", user.getEmail());

        return true;
    }

    public List<CourseMember> getCourseMemberships(String cwid) {
        return userRepo.getMembershipsByCwid(cwid);
    }

    @Transactional
    public List<Course> getEnrollments(String cwid) {
        return userRepo.getMembershipsByCwid(cwid).stream().map(CourseMember::getCourse).toList();
    }


}
