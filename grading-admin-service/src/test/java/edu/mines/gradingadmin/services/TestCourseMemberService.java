package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.config.ExternalServiceConfig;
import edu.mines.gradingadmin.containers.PostgresTestContainer;
import edu.mines.gradingadmin.managers.ImpersonationManager;
import edu.mines.gradingadmin.models.*;
import edu.mines.gradingadmin.models.tasks.UserSyncTaskDef;
import edu.mines.gradingadmin.repositories.CourseMemberRepo;
import edu.mines.gradingadmin.repositories.ScheduledTaskRepo;
import edu.mines.gradingadmin.repositories.SectionRepo;
import edu.mines.gradingadmin.repositories.UserRepo;
import edu.mines.gradingadmin.seeders.CanvasSeeder;
import edu.mines.gradingadmin.seeders.CourseSeeders;
import edu.mines.gradingadmin.seeders.UserSeeders;
import edu.mines.gradingadmin.services.external.CanvasService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;

import java.net.URI;
import java.util.Set;

@SpringBootTest
@Transactional
public class TestCourseMemberService implements PostgresTestContainer, CanvasSeeder {

    @Autowired
    private CourseMemberRepo courseMemberRepo;

    @Autowired
    private SectionRepo sectionRepo;
    @Autowired
    private UserRepo userRepo;

    private CanvasService canvasService;

    @Autowired
    private SectionService sectionService;

    @Autowired
    private ScheduledTaskRepo<UserSyncTaskDef> scheduledTaskRepo;

    @Autowired
    private CourseService courseService;

    @Autowired
    private ImpersonationManager impersonationManager;

    @Autowired
    private UserService userService;

    private CourseMemberService courseMemberService;
    @Autowired
    private CourseSeeders courseSeeders;
    @Autowired
    private UserSeeders userSeeders;


    @BeforeAll
    static void setupClass(){
        postgres.start();
    }

    @BeforeEach
    void setup(){
        // the sin of partial mocking
        canvasService = Mockito.spy(
                new CanvasService(null,
                new ExternalServiceConfig.CanvasConfig(true, URI.create("https://test.com"), "TeacherEnrollment", "StudentEnrollment", "TaEnrollmentEnrollment"))
        );

        courseMemberService = new CourseMemberService(
                courseMemberRepo, scheduledTaskRepo,
                userService, sectionService, courseService,
                canvasService, Mockito.mock(ApplicationEventPublisher.class),
                impersonationManager
        );

        applyMocks(canvasService);

    }

    @AfterEach
    void tearDown(){
        courseMemberRepo.deleteAll();
        sectionRepo.deleteAll();
        courseSeeders.clearAll();
        userSeeders.clearAll();
    }

    @Test
    void verifyAssignCourseMembers(){
        Course course = courseSeeders.course1(course1Id);
        User admin = userSeeders.admin1();

        Section section = sectionService.createSection(course1Section1Id, "Section A", course).orElseThrow(AssertionError::new);


        UserSyncTaskDef task = new UserSyncTaskDef();
        task.setCreatedByUser(admin);
        task.shouldAddNewUsers(true);
        task.setCourseToImport(course.getId());

        courseMemberService.syncCourseMembersTask(task);

        section = sectionService.getSection(section.getId()).orElseThrow(AssertionError::new);

        Set<CourseMember> members = courseMemberRepo.getAllByCourse(course);


        Assertions.assertEquals(course1Users.get().size(), members.size());

        for(var member : members){
            Assertions.assertEquals(course.getId(), member.getCourse().getId());
            Assertions.assertEquals(1, member.getSections().size());
            Assertions.assertTrue(member.getSections().contains(section));
        }

        // need to use an entity graph to get the lazy fields that we care about
        // todo: eval if we need to do this
//        Assertions.assertEquals(members.size(), section.getMembers().size());
    }

    @Test
    void verifyGetMembersByName() {
        Course course = courseSeeders.course1();
        User user = userSeeders.user1();

        // seed member
        CourseMember member = new CourseMember();
        member.setRole(CourseRole.STUDENT);
        member.setCanvasId("999999");
        member.setUser(user);
        member.setCourse(course);
        member = courseMemberRepo.save(member);
        course.setMembers(Set.of(member));

        User user2 = userSeeders.user2();

        // seed member
        CourseMember member2 = new CourseMember();
        member2.setRole(CourseRole.STUDENT);
        member2.setCanvasId("999999");
        member2.setUser(user2);
        member2.setCourse(course);
        member2 = courseMemberRepo.save(member2);
        course.setMembers(Set.of(member2));

        Set<CourseMember> foundMembers = courseMemberRepo.findAllByCourseByUserName(course, "User 2");
        Assertions.assertEquals(1, foundMembers.size());
    }

    @Test
    void verifyGetMembersByCwid() {
        Course course = courseSeeders.course1();
        User user = userSeeders.user1();

        // seed member
        CourseMember member = new CourseMember();
        member.setRole(CourseRole.STUDENT);
        member.setCanvasId("999999");
        member.setUser(user);
        member.setCourse(course);
        member = courseMemberRepo.save(member);
        course.setMembers(Set.of(member));

        User user2 = userSeeders.user2();

        // seed member
        CourseMember member2 = new CourseMember();
        member2.setRole(CourseRole.STUDENT);
        member2.setCanvasId("999999");
        member2.setUser(user2);
        member2.setCourse(course);
        member2 = courseMemberRepo.save(member2);
        course.setMembers(Set.of(member2));

        Set<CourseMember> foundMembers = courseMemberRepo.findAllByCourseByCwid(course, "80000001");
        Assertions.assertEquals(1, foundMembers.size());
    }

    @Test
    void verifyGetMembers() {
        Course course = courseSeeders.course1();
        User user = userSeeders.user1();

        // seed member
        CourseMember member = new CourseMember();
        member.setRole(CourseRole.STUDENT);
        member.setCanvasId("999999");
        member.setUser(user);
        member.setCourse(course);
        member = courseMemberRepo.save(member);
        course.setMembers(Set.of(member));

        User user2 = userSeeders.user2();

        // seed member
        CourseMember member2 = new CourseMember();
        member2.setRole(CourseRole.STUDENT);
        member2.setCanvasId("999999");
        member2.setUser(user2);
        member2.setCourse(course);
        member2 = courseMemberRepo.save(member2);
        course.setMembers(Set.of(member2));

        Set<CourseMember> foundMembers = courseMemberRepo.getAllByCourse(course);
        Assertions.assertEquals(2, foundMembers.size());
    }

    @Test
    void verifyAddMemberToCourse() {
        Course course = courseSeeders.course1();
        User user = userSeeders.user1();

        courseMemberService.addMemberToCourse(course.getId().toString(), user.getCwid(), "234989", CourseRole.STUDENT);

        Set<CourseMember> foundMembers = courseMemberRepo.getAllByCourse(course);
        Assertions.assertEquals(1, foundMembers.size());
    }
}
