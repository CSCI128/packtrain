package edu.mines.packtrain.services;

import edu.mines.packtrain.containers.PostgresTestContainer;
import edu.mines.packtrain.data.CourseMemberDTO;
import edu.mines.packtrain.managers.ImpersonationManager;
import edu.mines.packtrain.models.Course;
import edu.mines.packtrain.models.enums.CourseRole;
import edu.mines.packtrain.models.User;
import edu.mines.packtrain.models.tasks.UserSyncTaskDef;
import edu.mines.packtrain.repositories.CourseMemberRepo;
import edu.mines.packtrain.repositories.ScheduledTaskRepo;
import edu.mines.packtrain.repositories.UserRepo;
import edu.mines.packtrain.seeders.CanvasSeeder;
import edu.mines.packtrain.seeders.CourseSeeders;
import edu.mines.packtrain.seeders.UserSeeders;
import edu.mines.packtrain.services.external.CanvasService;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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

    CourseMemberService courseMemberService;
    @Autowired
    private CourseSeeders courseSeeders;

    private CanvasService canvasService;

    @Autowired
    private SectionService sectionService;

    @Autowired
    private ScheduledTaskRepo<UserSyncTaskDef> scheduledTaskRepo;

    @Autowired
    private CourseService courseService;

    @Autowired
    private ImpersonationManager impersonationManager;

    @BeforeAll
    static void setupClass() {
        postgres.start();
    }

    @BeforeEach
    void setup(){
        courseMemberService = new CourseMemberService(
                courseMemberRepo, scheduledTaskRepo,
                userService, sectionService, courseService,
                canvasService, Mockito.mock(ApplicationEventPublisher.class),
                impersonationManager
        );
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
        User admin = userSeeders.admin1();
        User user1 = userSeeders.user1();

        userService.makeAdmin(admin, user1.getCwid());

        User user = userService.getUserByCwid(user1.getCwid());

        Assertions.assertTrue(user.isAdmin());
        Assertions.assertEquals(user1.getCwid(), user.getCwid());
    }

    @Test
    void verifyMakeAdminTeacherMembership(){
        User admin = userSeeders.admin1();
        User user1 = userSeeders.user1();

        Course course1 = courseSeeders.course1();

        courseMemberService.addMemberToCourse(course1.getId().toString(), new CourseMemberDTO().cwid(user1.getCwid()).canvasId("99999").courseRole(CourseMemberDTO.CourseRoleEnum.fromValue(CourseRole.INSTRUCTOR.getRole())));

        userService.makeAdmin(admin, user1.getCwid());

        User user = userService.getUserByCwid(user1.getCwid());

        Assertions.assertEquals(user1.getCwid(), user.getCwid());
        Assertions.assertTrue(user.isAdmin());
    }

    @Test
    void verifyMakeAdminStudentMembership(){
        User admin = userSeeders.admin1();
        User user1 = userSeeders.user1();

        Course course1 = courseSeeders.course1();

        courseMemberService.addMemberToCourse(course1.getId().toString(), new CourseMemberDTO().cwid(user1.getCwid()).canvasId("99999").courseRole(CourseMemberDTO.CourseRoleEnum.fromValue(CourseRole.STUDENT.getRole())));

        ResponseStatusException exception = Assertions.assertThrows(ResponseStatusException.class, () -> userService.makeAdmin(admin, user1.getCwid()));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void verifyGetEnrollments() {
        User user1 = userSeeders.user1();
        Course course1 = courseSeeders.course1();
        Course course2 = courseSeeders.course2();

        courseMemberService.addMemberToCourse(course1.getId().toString(), new CourseMemberDTO().cwid(user1.getCwid()).canvasId("99999").courseRole(CourseMemberDTO.CourseRoleEnum.fromValue(CourseRole.STUDENT.getRole())));
        courseMemberService.addMemberToCourse(course2.getId().toString(), new CourseMemberDTO().cwid(user1.getCwid()).canvasId("99991").courseRole(CourseMemberDTO.CourseRoleEnum.fromValue(CourseRole.STUDENT.getRole())));

        List<Course> enrollments = userService.getEnrollments(user1.getCwid());
        Assertions.assertEquals(2, enrollments.size());
    }
}
