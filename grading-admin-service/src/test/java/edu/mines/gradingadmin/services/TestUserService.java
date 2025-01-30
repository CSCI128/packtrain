package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.containers.PostgresTestContainer;
import edu.mines.gradingadmin.models.User;
import edu.mines.gradingadmin.repositories.UserRepo;
import edu.mines.gradingadmin.seeders.UserSeeders;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SpringBootTest
public class TestUserService implements PostgresTestContainer {

    @Autowired
    UserRepo userRepo;

    @Autowired
    UserSeeders userSeeders;

    @Autowired
    UserService userService;

    @BeforeAll
    static void setupClass() {
        postgres.start();
    }

    @AfterEach
    void tearDown(){
        userRepo.deleteAll();
    }

    @Test
    void verifyCreateNewUserDoesntExistWithOauth(){
        List<User> users = userRepo.getAll();

        Assertions.assertEquals(0, users.size());

        Optional<User> user = userService.createNewUser("99999999", false, "Test User", "test@test.com", UUID.randomUUID().toString());

        Assertions.assertFalse(user.isEmpty());

        users = userRepo.getAll();

        Assertions.assertEquals(1, users.size());
    }

    @Test
    void verifyLinkCwidToOauthId(){
        User user = userSeeders.user1();

        String id = UUID.randomUUID().toString();

        Optional<User> userRes = userService.linkCwidToOauthId(user, id);

        Assertions.assertFalse(userRes.isEmpty());

        List<User> users = userRepo.getAll();

        Assertions.assertEquals(1, users.size());

        Assertions.assertEquals(id, users.getFirst().getOAuthId().toString());
    }




}
