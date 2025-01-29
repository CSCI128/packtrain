package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.models.User;
import edu.mines.gradingadmin.repositories.UserRepo;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import javax.swing.text.html.Option;
import java.util.Optional;
import java.util.UUID;

@Service
@Cacheable("users")
public class UserService {
    private final UserRepo userRepo;

    public UserService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    public Optional<User> getUserByCwid(String cwid){
        return userRepo.getByCwid(cwid);
    }

    public Optional<User> getUserByOauthId(String id){
        return userRepo.getByOauthId(UUID.fromString(id));
    }

    public Optional<User> linkCwidToOauthId(User user, String id){
        user.setOauthId(UUID.fromString(id));

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

    public Optional<User> createNewUser(String cwid, boolean isAdmin, String name, String email){
        if (userRepo.existsByCwid(cwid)){
            return Optional.empty();
        }

        User user = new User();

        user.setCwid(cwid);
        user.setName(name);
        user.setEmail(email);
        user.setAdmin(isAdmin);
        user.setUser(true);

        return Optional.of(userRepo.save(user));
    }

    public Optional<User> createNewUser(String cwid, boolean isAdmin, String name, String email, String oauthId){
        if (userRepo.existsByCwid(cwid)){
            return Optional.empty();
        }

        User user = new User();

        user.setCwid(cwid);
        user.setOauthId(UUID.fromString(oauthId));
        user.setName(name);
        user.setEmail(email);
        user.setAdmin(isAdmin);
        user.setUser(true);

        return Optional.of(userRepo.save(user));
    }

    public Optional<User> disableUser(String cwidToDisable){
        Optional<User> user = getUserByCwid(cwidToDisable);

        if (user.isEmpty()){
            return Optional.empty();
        }

        user.get().setUser(false);

        return Optional.of(userRepo.save(user.get()));

    }

    public Optional<User> enableUser(String cwidToDisable){
        Optional<User> user = getUserByCwid(cwidToDisable);

        if (user.isEmpty()){
            return Optional.empty();
        }

        user.get().setUser(true);

        return Optional.of(userRepo.save(user.get()));
    }


}
