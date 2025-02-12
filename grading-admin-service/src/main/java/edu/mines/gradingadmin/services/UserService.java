package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.models.User;
import edu.mines.gradingadmin.repositories.UserRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Cacheable("users")
@Slf4j
public class UserService {
    private final UserRepo userRepo;

    public UserService(UserRepo userRepo) {
        this.userRepo = userRepo;
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

    public Optional<User> updateUser(String cwid, boolean isAdmin, String name, String email){
        Optional<User> user = getUserByCwid(cwid);

        if (user.isEmpty()){
            return Optional.empty();
        }

        user.get().setEmail(email);
        user.get().setName(name);
        user.get().setAdmin(isAdmin);


        return Optional.of(userRepo.save(user.get()));
    }

    public List<User> getOrCreateUsersFromCanvas(Map<String, edu.ksu.canvas.model.User> canvasUsers){
        List<User> users = new LinkedList<>();

        for (var user : canvasUsers.values()){
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

    public Optional<User> disableUser(String cwidToDisable){
        Optional<User> user = getUserByCwid(cwidToDisable);

        if (user.isEmpty()){
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


}
