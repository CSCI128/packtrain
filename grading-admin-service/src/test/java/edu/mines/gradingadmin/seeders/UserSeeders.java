package edu.mines.gradingadmin.seeders;

import edu.mines.gradingadmin.models.User;
import edu.mines.gradingadmin.repositories.UserRepo;
import org.springframework.stereotype.Service;

@Service
public class UserSeeders {
    private final UserRepo repo;


    public UserSeeders(UserRepo repo) {
        this.repo = repo;
    }

    public User user1(){
        User user = new User();
        user.setName("User 1");
        user.setEmail("user1@test.com");
        user.setCwid("80000001");
        user.setEnabled(true);

        return repo.save(user);
    }

    public User user2(){
        User user = new User();
        user.setName("User 2");
        user.setEmail("user2@test.com");
        user.setCwid("80000002");
        user.setEnabled(true);

        return repo.save(user);
    }

    public User admin1(){
        User user = new User();
        user.setName("Admin 1");
        user.setEmail("admin1@test.com");
        user.setCwid("90000001");
        user.setEnabled(true);
        user.setAdmin(true);

        return repo.save(user);
    }

    public void clearAll(){
        repo.deleteAll();
    }
}
