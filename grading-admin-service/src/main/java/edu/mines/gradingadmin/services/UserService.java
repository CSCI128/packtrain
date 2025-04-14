package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.data.UserDTO;
import edu.mines.gradingadmin.models.Course;
import edu.mines.gradingadmin.models.CourseMember;
import edu.mines.gradingadmin.models.enums.CourseRole;
import edu.mines.gradingadmin.models.User;
import edu.mines.gradingadmin.repositories.CourseMemberRepo;
import edu.mines.gradingadmin.repositories.UserRepo;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Cacheable("users")
@Slf4j
public class UserService {
    private final UserRepo userRepo;
    private final CourseMemberRepo courseMemberRepo;

    public UserService(UserRepo userRepo, CourseMemberRepo courseMemberRepo) {
        this.userRepo = userRepo;
        this.courseMemberRepo = courseMemberRepo;
    }

    public Optional<User> getUserByCwid(String cwid){
        return userRepo.getByCwid(cwid);
    }

    public Optional<User> getUserByOauthId(String id){
        return userRepo.getByOAuthId(UUID.fromString(id));
    }

    public Optional<User> linkCwidToOauthId(User user, String id){
        user.setOAuthId(UUID.fromString(id));

        return Optional.of(userRepo.save(user));
    }

    public List<User> getAllUsers(){
        return userRepo.getAll();
    }

    public Optional<User> updateUser(UserDTO userDTO){
        Optional<User> user = getUserByCwid(userDTO.getCwid());

        if (user.isEmpty()) {
            return Optional.empty();
        }

        user.get().setEmail(userDTO.getEmail());
        user.get().setName(userDTO.getName());

        return Optional.of(userRepo.save(user.get()));
    }

    public List<User> getOrCreateUsersFromCanvas(Map<String, edu.ksu.canvas.model.User> canvasUsers){
        List<User> users = new LinkedList<>();

        for (edu.ksu.canvas.model.User user : canvasUsers.values()){
            Optional<User> newUser = getUserByCwid(user.getSisUserId())
                    .or(() -> createNewUser(user.getSisUserId(), false, user.getName(), user.getEmail()));

            if (newUser.isEmpty()){
                log.warn("Failed to look up or create user {}!", user.getEmail());
                continue;
            }

            users.add(newUser.get());
        }

        if (users.size() != canvasUsers.size()){
            log.warn("Not all users were created successfully! Expected {} users. Only {} users were created successfully", canvasUsers.size(), users.size());
        }

        log.info("Created {} users", users.size());

        return users;
    }

    public Optional<User> createNewUser(String cwid, boolean isAdmin, String name, String email){
        if (userRepo.existsByCwid(cwid)){
            return Optional.empty();
        }

        User user = new User();

        user.setCwid(cwid);
        user.setName(name);
        user.setEmail(email);
        user.setAdmin(isAdmin);
        user.setEnabled(true);

        log.debug("Created new user: {}", user);

        return Optional.of(userRepo.save(user));
    }

    public Optional<User> createNewUser(String cwid, boolean isAdmin, String name, String email, String oauthId){
        if (userRepo.existsByCwid(cwid)){
            return Optional.empty();
        }

        User user = new User();

        user.setCwid(cwid);
        user.setOAuthId(UUID.fromString(oauthId));
        user.setName(name);
        user.setEmail(email);
        user.setAdmin(isAdmin);
        user.setEnabled(true);

        log.debug("Created new user with oauth id: {}", user);

        return Optional.of(userRepo.save(user));
    }

    public Optional<User> disableUser(User actingUser, String cwidToDisable){
        if (actingUser.getCwid().equals(cwidToDisable)){
            log.warn("Attempt to disable current user '{}' from admin!", cwidToDisable);
            return Optional.empty();
        }

        Optional<User> user = getUserByCwid(cwidToDisable);

        if (user.isEmpty()){
            return Optional.empty();
        }

        if (user.get().isAdmin()){
            log.warn("Attempt to disable admin user '{}'", cwidToDisable);
            return Optional.empty();
        }

        user.get().setEnabled(false);

        return Optional.of(userRepo.save(user.get()));
    }

    public Optional<User> enableUser(String cwidToEnable){
        Optional<User> user = getUserByCwid(cwidToEnable);

        if (user.isEmpty()){
            return Optional.empty();
        }

        user.get().setEnabled(true);

        return Optional.of(userRepo.save(user.get()));
    }

    @Transactional
    public Optional<User> makeAdmin(String cwidToMakeAdmin){
        Optional<User> user = getUserByCwid(cwidToMakeAdmin);

        if (user.isEmpty()){
            return Optional.empty();
        }

        boolean isStudent = user.get().getCourseMemberships().stream()
                .anyMatch(u -> u.getRole() == CourseRole.STUDENT);

        if (isStudent){
            log.warn("Attempt to make student '{}' admin!", user.get().getEmail());
            return Optional.empty();
        }

        user.get().setAdmin(true);

        log.info("Made user '{}' an admin", user.get().getEmail());

        return Optional.of(userRepo.save(user.get()));
    }

    @Transactional
    public Optional<User> demoteAdmin(User actingUser, String cwid) {
        if (actingUser.getCwid().equals(cwid)){
            log.warn("Attempt to demote current user '{}' from admin!", cwid);
            return Optional.empty();
        }
        Optional<User> user = getUserByCwid(cwid);

        if (user.isEmpty()){
            return Optional.empty();
        }

        user.get().setAdmin(false);

        log.info("Demoted admin '{}' to user", user.get().getEmail());

        return Optional.of(userRepo.save(user.get()));
    }

    @Transactional
    public List<CourseMember> getCourseMemberships(String cwid) {
        Optional<User> user = userRepo.getByCwid(cwid);
        if (user.isEmpty()){
            return List.of();
        }
        return userRepo.getUserEnrollmentsById(user.get().getCwid());
    }

    @Transactional
    public List<Course> getEnrollments(String cwid) {
        return courseMemberRepo.getEnabledCoursesByUserId(cwid);
    }


}
