package edu.mines.gradingadmin.managers;

import edu.mines.gradingadmin.containers.PostgresTestContainer;
import edu.mines.gradingadmin.models.User;
import edu.mines.gradingadmin.repositories.UserRepo;
import edu.mines.gradingadmin.services.CredentialService;
import edu.mines.gradingadmin.services.UserService;
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
    private UserService userService;

    @Autowired
    private CredentialService credentialService;

    @BeforeAll
    static void setupClass() {
        postgres.start();
    }

    @AfterEach
    void tearDown(){
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

        SecurityManager manager = new SecurityManager(userService, credentialService);

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

        SecurityManager manager = new SecurityManager(userService, credentialService);

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

        SecurityManager manager = new SecurityManager(userService, credentialService);

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

        SecurityManager manager = new SecurityManager(userService, credentialService);

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

        SecurityManager manager = new SecurityManager(userService, credentialService);

        manager.setPrincipalFromRequest(request);

        Assertions.assertThrows(AccessDeniedException.class, manager::readUserFromRequest);
    }




}
