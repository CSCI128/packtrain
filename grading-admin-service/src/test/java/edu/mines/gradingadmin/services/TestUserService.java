package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.containers.PostgresTestContainer;
import edu.mines.gradingadmin.models.Course;
import edu.mines.gradingadmin.models.CourseRole;
import edu.mines.gradingadmin.models.User;
import edu.mines.gradingadmin.repositories.CourseMemberRepo;
import edu.mines.gradingadmin.repositories.UserRepo;
import edu.mines.gradingadmin.seeders.CanvasSeeder;
import edu.mines.gradingadmin.seeders.CourseSeeders;
import edu.mines.gradingadmin.seeders.UserSeeders;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.shaded.org.checkerframework.checker.units.qual.A;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SpringBootTest
public class TestUserService implements PostgresTestContainer, CanvasSeeder {

    @Autowired
    UserRepo userRepo;

    @Autowired
    CourseMemberRepo courseMemberRepo;

    @Autowired
    UserSeeders userSeeders;

    @Autowired
    UserService userService;

    @Autowired
    CourseMemberService courseMemberService;
    @Autowired
    private CourseSeeders courseSeeders;

    @BeforeAll
    static void setupClass() {
        postgres.start();
    }

    @AfterEach
    void tearDown(){
        courseMemberRepo.deleteAll();
        userRepo.deleteAll();
        courseSeeders.clearAll();
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

    @Test
    void verifyCreateNewUsersFromCanvas(){
        List<User> users = userRepo.getAll();

        Assertions.assertTrue(users.isEmpty());

        users = userService.getOrCreateUsersFromCanvas(course1Users.get());

        for (var user : users) {
            Assertions.assertTrue(userRepo.existsByCwid(user.getCwid()));
        }

    }

    @Test
    void verifyDontCreateUsersIfExistFromCanvas(){
        List<User> users = userRepo.getAll();

        Assertions.assertTrue(users.isEmpty());

        userService.getOrCreateUsersFromCanvas(course1Users.get());
        userService.getOrCreateUsersFromCanvas(course1Users.get());

        Assertions.assertEquals(course1Users.get().size(), userRepo.getAll().size());

    }

    @Test
    void verifyMakeAdminNoMemberships(){
        User user1 = userSeeders.user1();

        userService.makeAdmin(user1.getCwid());

        Optional<User> user = userService.getUserByCwid(user1.getCwid());
        Assertions.assertTrue(user.isPresent());

        Assertions.assertTrue(user.get().isAdmin());
        Assertions.assertEquals(user1.getCwid(), user.get().getCwid());
    }

    @Test
    void verifyMakeAdminTeacherMembership(){
        User user1 = userSeeders.user1();

        Course course1 = courseSeeders.course1();

        courseMemberService.addMemberToCourse(course1.getId().toString(), user1.getCwid(), "99999", CourseRole.INSTRUCTOR);

        userService.makeAdmin(user1.getCwid());

        Optional<User> user = userService.getUserByCwid(user1.getCwid());
        Assertions.assertTrue(user.isPresent());

        Assertions.assertTrue(user.get().isAdmin());
        Assertions.assertEquals(user1.getCwid(), user.get().getCwid());
    }

    @Test
    void verifyMakeAdminStudentMembership(){
        User user1 = userSeeders.user1();

        Course course1 = courseSeeders.course1();

        courseMemberService.addMemberToCourse(course1.getId().toString(), user1.getCwid(), "99999", CourseRole.STUDENT);

        userService.makeAdmin(user1.getCwid());

        Optional<User> user = userService.getUserByCwid(user1.getCwid());
        Assertions.assertTrue(user.isPresent());

        Assertions.assertFalse(user.get().isAdmin());
        Assertions.assertEquals(user1.getCwid(), user.get().getCwid());
    }



}
