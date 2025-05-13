package edu.mines.packtrain.managers;

import edu.mines.packtrain.containers.PostgresTestContainer;
import edu.mines.packtrain.data.CredentialDTO;
import edu.mines.packtrain.models.*;
import edu.mines.packtrain.models.enums.CourseRole;
import edu.mines.packtrain.models.enums.CredentialType;
import edu.mines.packtrain.repositories.CourseMemberRepo;
import edu.mines.packtrain.repositories.CredentialRepo;
import edu.mines.packtrain.repositories.UserRepo;
import edu.mines.packtrain.seeders.CourseSeeders;
import edu.mines.packtrain.services.CourseMemberService;
import edu.mines.packtrain.services.CredentialService;
import edu.mines.packtrain.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@SpringBootTest
public class TestSecurityManager implements PostgresTestContainer {

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private CredentialRepo credentialRepo;

    @Autowired
    private CourseMemberRepo courseMemberRepo;

    @Autowired
    private UserService userService;

    @Autowired
    private CourseSeeders courseSeeders;

    @Autowired
    private CredentialService credentialService;

    @Autowired
    private CourseMemberService courseMemberService;


    @BeforeAll
    static void setupClass() {
        postgres.start();
    }

    @AfterEach
    void tearDown(){
        courseSeeders.clearAll();
        credentialRepo.deleteAll();
        userRepo.deleteAll();
    }

    HttpServletRequest requestFactory(Jwt token){
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        JwtAuthenticationToken principal = new JwtAuthenticationToken(token);

        Mockito.when(request.getUserPrincipal()).thenReturn(principal);

        return request;
    }

    @Test
    void verifyUserCreatedWhenNotExist(){
        String cwid = "99999999";
        String sub = UUID.randomUUID().toString();

        Jwt token = new Jwt("token", null, null, Map.of("alg", "none"), Map.of(
                "sub", sub,
                "cwid", cwid,
                "email", "test@test.com",
                "is_admin", false,
                "name", "Test User"
        ));

        HttpServletRequest request = requestFactory(token);

        SecurityManager manager = new SecurityManager(userService, credentialService, courseMemberService);

        manager.setPrincipalFromRequest(request);

        manager.readUserFromRequest();

        User user = manager.getUser();

        Assertions.assertEquals(sub, user.getOAuthId().toString());
        Assertions.assertEquals(cwid, user.getCwid());

        List<User> users = userRepo.getAll();

        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals(cwid, users.getFirst().getCwid());
    }

    @Test
    void verifyUserLinkedWhenUnlinked(){
        String cwid = "99999999";
        String sub = UUID.randomUUID().toString();

        User user = new User();
        user.setCwid(cwid);
        user.setEmail("test@test.com");
        user.setName("Test User");
        userRepo.save(user);

        Jwt token = new Jwt("token", null, null, Map.of("alg", "none"), Map.of(
                "sub", sub,
                "cwid", cwid,
                "email", "test@test.com",
                "is_admin", false,
                "name", "Test User"
        ));

        HttpServletRequest request = requestFactory(token);

        SecurityManager manager = new SecurityManager(userService, credentialService, courseMemberService);

        manager.setPrincipalFromRequest(request);

        manager.readUserFromRequest();

        Assertions.assertEquals(sub, manager.getUser().getOAuthId().toString());
        Assertions.assertEquals(cwid, manager.getUser().getCwid());

        List<User> users = userRepo.getAll();

        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals(sub, users.getFirst().getOAuthId().toString());
    }

    @Test
    void verifyUserRecognizedWhenLinkedAndExists(){
        String cwid = "99999999";
        String sub = UUID.randomUUID().toString();

        User user = new User();
        user.setOAuthId(UUID.fromString(sub));
        user.setCwid(cwid);
        user.setEmail("test@test.com");
        user.setName("Test User");
        userRepo.save(user);

        Jwt token = new Jwt("token", null, null, Map.of("alg", "none"), Map.of(
                "sub", sub,
                "cwid", cwid,
                "email", "test@test.com",
                "is_admin", false,
                "name", "Test User"
        ));

        HttpServletRequest request = requestFactory(token);

        SecurityManager manager = new SecurityManager(userService, credentialService, courseMemberService);

        manager.setPrincipalFromRequest(request);

        manager.readUserFromRequest();
        List<User> users = userRepo.getAll();

        Assertions.assertEquals(1, users.size());

    }

    @Test
    void verifyFailToCreateUserWhenMissingClaims(){
        String cwid = "99999999";
        String sub = UUID.randomUUID().toString();

        Jwt token = new Jwt("token", null, null, Map.of("alg", "none"), Map.of(
                "sub", sub,
                "cwid", cwid,
                "is_admin", false,
                "name", "Test User"
        ));

        HttpServletRequest request = requestFactory(token);

        SecurityManager manager = new SecurityManager(userService, credentialService, courseMemberService);

        manager.setPrincipalFromRequest(request);

        Assertions.assertThrows(AccessDeniedException.class, manager::readUserFromRequest);

        List<User> users = userRepo.getAll();

        Assertions.assertEquals(0, users.size());

    }

    @Test
    void verifyRefuseToRecognizeUserWhenMissingClaims(){
        String cwid = "99999999";
        String sub = UUID.randomUUID().toString();

        Jwt token = new Jwt("token", null, null, Map.of("alg", "none"), Map.of(
                "sub", sub,
                "email", "test@test.com",
                "is_admin", false,
                "name", "Test User"
        ));

        HttpServletRequest request = requestFactory(token);

        SecurityManager manager = new SecurityManager(userService, credentialService, courseMemberService);

        manager.setPrincipalFromRequest(request);

        Assertions.assertThrows(AccessDeniedException.class, manager::readUserFromRequest);
    }

    @Test
    void verifyGetCredentialsExists(){
        String cwid = "99999999";
        String sub = UUID.randomUUID().toString();

        User user = new User();
        user.setOAuthId(UUID.fromString(sub));
        user.setCwid(cwid);
        user.setEmail("test@test.com");
        user.setName("Test User");
        userRepo.save(user);

        Jwt token = new Jwt("token", null, null, Map.of("alg", "none"), Map.of(
                "sub", sub,
                "cwid", cwid,
                "email", "test@test.com",
                "is_admin", false,
                "name", "Test User"
        ));

        String credential = "supersecure";

        credentialService.createNewCredentialForService(cwid, new CredentialDTO().name("canvas").apiKey(credential).service(CredentialDTO.ServiceEnum.fromValue("canvas")));

        HttpServletRequest request = requestFactory(token);

        SecurityManager manager = new SecurityManager(userService, credentialService, courseMemberService);

        manager.setPrincipalFromRequest(request);
        manager.readUserFromRequest();

        String actual = manager.getCredential(CredentialType.CANVAS, UUID.randomUUID());

        Assertions.assertEquals(credential, actual);
    }

    @Test
    void verifyGetCredentialsDNE(){
        String cwid = "99999999";
        String sub = UUID.randomUUID().toString();

        User user = new User();
        user.setOAuthId(UUID.fromString(sub));
        user.setCwid(cwid);
        user.setEmail("test@test.com");
        user.setName("Test User");
        userRepo.save(user);

        SecurityManager manager = new SecurityManager(userService, credentialService, courseMemberService);
        Jwt token = new Jwt("token", null, null, Map.of("alg", "none"), Map.of(
                "sub", sub,
                "cwid", cwid,
                "email", "test@test.com",
                "is_admin", false,
                "name", "Test User"
        ));

        HttpServletRequest request = requestFactory(token);

        manager.setPrincipalFromRequest(request);
        manager.readUserFromRequest();

        Assertions.assertThrows(AccessDeniedException.class, () -> manager.getCredential(CredentialType.CANVAS, UUID.randomUUID()));
    }


    @Test
    void verifyCheckEnrollmentMatch(){
        String cwid = "99999999";
        String sub = UUID.randomUUID().toString();

        User user = new User();
        user.setOAuthId(UUID.fromString(sub));
        user.setCwid(cwid);
        user.setEmail("test@test.com");
        user.setName("Test User");
        user = userRepo.save(user);

        Course course = courseSeeders.course1();

        CourseMember membership = new CourseMember();
        membership.setCanvasId("9999999");
        membership.setRole(CourseRole.INSTRUCTOR);
        membership.setUser(user);
        membership.setCourse(course);

        membership = courseMemberRepo.save(membership);

        SecurityManager manager = new SecurityManager(userService, credentialService, courseMemberService);
        Jwt token = new Jwt("token", null, null, Map.of("alg", "none"), Map.of(
                "sub", sub,
                "cwid", cwid,
                "email", "test@test.com",
                "is_admin", false,
                "name", "Test User"
        ));

        HttpServletRequest request = requestFactory(token);

        manager.setPrincipalFromRequest(request);
        manager.readUserFromRequest();

        Assertions.assertTrue(manager.hasCourseMembership(CourseRole.INSTRUCTOR, course.getId()));
    }

    @Test
    void verifyCheckEnrollmentNoMatch(){
        String cwid = "99999999";
        String sub = UUID.randomUUID().toString();

        User user = new User();
        user.setOAuthId(UUID.fromString(sub));
        user.setCwid(cwid);
        user.setEmail("test@test.com");
        user.setName("Test User");
        user = userRepo.save(user);

        Course course = courseSeeders.course1();

        CourseMember membership = new CourseMember();
        membership.setCanvasId("9999999");
        membership.setRole(CourseRole.INSTRUCTOR);
        membership.setUser(user);
        membership.setCourse(course);

        membership = courseMemberRepo.save(membership);

        SecurityManager manager = new SecurityManager(userService, credentialService, courseMemberService);
        Jwt token = new Jwt("token", null, null, Map.of("alg", "none"), Map.of(
                "sub", sub,
                "cwid", cwid,
                "email", "test@test.com",
                "is_admin", false,
                "name", "Test User"
        ));

        HttpServletRequest request = requestFactory(token);

        manager.setPrincipalFromRequest(request);
        manager.readUserFromRequest();

        Assertions.assertFalse(manager.hasCourseMembership(CourseRole.STUDENT, course.getId()));
    }

    @Test
    void verifyCheckEnrollmentDNE(){
        String cwid = "99999999";
        String sub = UUID.randomUUID().toString();

        User user = new User();
        user.setOAuthId(UUID.fromString(sub));
        user.setCwid(cwid);
        user.setEmail("test@test.com");
        user.setName("Test User");
        user = userRepo.save(user);

        Course course = courseSeeders.course1();

        SecurityManager manager = new SecurityManager(userService, credentialService, courseMemberService);
        Jwt token = new Jwt("token", null, null, Map.of("alg", "none"), Map.of(
                "sub", sub,
                "cwid", cwid,
                "email", "test@test.com",
                "is_admin", false,
                "name", "Test User"
        ));

        HttpServletRequest request = requestFactory(token);

        manager.setPrincipalFromRequest(request);
        manager.readUserFromRequest();

        Assertions.assertFalse(manager.hasCourseMembership(CourseRole.STUDENT, course.getId()));
    }

}
