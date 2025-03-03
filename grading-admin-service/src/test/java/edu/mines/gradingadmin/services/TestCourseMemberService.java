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
        courseSeeders.clearAll();
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

    }

    @Test
    void verifyDuplicatesAreIgnored(){
        Course course = courseSeeders.course1(course1Id);
        User admin = userSeeders.admin1();

        sectionService.createSection(course1Section1Id, "Section A", course).orElseThrow(AssertionError::new);

        UserSyncTaskDef task = new UserSyncTaskDef();
        task.setCreatedByUser(admin);
        task.shouldAddNewUsers(true);
        task.setCourseToImport(course.getId());

        courseMemberService.syncCourseMembersTask(task);

        courseMemberService.syncCourseMembersTask(task);

        Set<CourseMember> members = courseMemberRepo.getAllByCourse(course);

        Assertions.assertEquals(course1Users.get().size(), members.size());
    }

    @Test
    void verifyDuplicatesAreUpdated(){
        Course course = courseSeeders.course1(course1Id);
        User admin = userSeeders.admin1();

        sectionService.createSection(course1Section1Id, "Section A", course).orElseThrow(AssertionError::new);

        UserSyncTaskDef task = new UserSyncTaskDef();
        task.setCreatedByUser(admin);
        task.shouldAddNewUsers(true);
        task.shouldUpdateExistingUsers(true);
        task.setCourseToImport(course.getId());

        courseMemberService.syncCourseMembersTask(task);

        courseMemberService.syncCourseMembersTask(task);

        Set<CourseMember> members = courseMemberRepo.getAllByCourse(course);

        Assertions.assertEquals(course1Users.get().size(), members.size());
    }

    @Test
    void verifyRemoveOldMembers(){
        Course course = courseSeeders.course1(course1Id);
        User admin = userSeeders.admin1();
        User user = userSeeders.user(instructor2.get().getName(), instructor2.get().getEmail(), instructor2.get().getSisUserId());

        sectionService.createSection(course1Section1Id, "Section A", course).orElseThrow(AssertionError::new);

        courseMemberService.addMemberToCourse(course.getId().toString(), user.getCwid(), String.valueOf(instructor2.get().getId()), CourseRole.INSTRUCTOR);

        Set<CourseMember> members = courseMemberRepo.getAllByCourse(course);

        Assertions.assertEquals(1, members.size());

        UserSyncTaskDef task = new UserSyncTaskDef();
        task.setCreatedByUser(admin);
        task.shouldRemoveOldUsers(true);
        task.setCourseToImport(course.getId());

        courseMemberService.syncCourseMembersTask(task);

        members = courseMemberRepo.getAllByCourse(course);

        Assertions.assertTrue(members.isEmpty());
    }

    @Test
    void verifyRemoveMembership(){
        Course course = courseSeeders.course1(course1Id);
        User user = userSeeders.user(instructor2.get().getName(), instructor2.get().getEmail(), instructor2.get().getSisUserId());
        courseMemberService.addMemberToCourse(course.getId().toString(), user.getCwid(), String.valueOf(instructor2.get().getId()), CourseRole.INSTRUCTOR);

        Set<CourseMember> members = courseMemberRepo.getAllByCourse(course);

        Assertions.assertEquals(1, members.size());

        Assertions.assertTrue(courseMemberService.removeMembershipForUserAndCourse(user, course.getId().toString()));

        members = courseMemberRepo.getAllByCourse(course);

        Assertions.assertTrue(members.isEmpty());
    }

    @Test
    void verifyCantRemoveOwner(){
        Course course = courseSeeders.course1(course1Id);
        User user = userSeeders.user(instructor2.get().getName(), instructor2.get().getEmail(), instructor2.get().getSisUserId());
        courseMemberService.addMemberToCourse(course.getId().toString(), user.getCwid(), String.valueOf(instructor2.get().getId()), CourseRole.OWNER);

        Set<CourseMember> members = courseMemberRepo.getAllByCourse(course);

        Assertions.assertEquals(1, members.size());

        Assertions.assertFalse(courseMemberService.removeMembershipForUserAndCourse(user, course.getId().toString()));

        members = courseMemberRepo.getAllByCourse(course);

        Assertions.assertFalse(members.isEmpty());

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
