package edu.mines.packtrain.services;

import edu.mines.packtrain.containers.PostgresTestContainer;
import edu.mines.packtrain.data.CredentialDTO;
import edu.mines.packtrain.models.Credential;
import edu.mines.packtrain.models.enums.CredentialType;
import edu.mines.packtrain.models.User;
import edu.mines.packtrain.repositories.CredentialRepo;
import edu.mines.packtrain.seeders.CourseSeeders;
import edu.mines.packtrain.seeders.UserSeeders;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@SpringBootTest
@Transactional
class TestCredentialService implements PostgresTestContainer {
    @Autowired
    UserSeeders userSeeder;
    @Autowired
    CourseSeeders courseSeeders;
    @Autowired
    CredentialRepo credentialRepo;

    @Autowired
    CredentialService credentialService;

    @BeforeAll
    static void setupClass() {
        postgres.start();

    }

    @AfterEach
    void tearDown(){
        credentialRepo.deleteAll();
        userSeeder.clearAll();
        courseSeeders.clearAll();
    }


    @Test
    void verifyCreateNewCredentialDoesNotExist(){
        User user = userSeeder.user1();

        List<Credential> credentials = credentialRepo.getByCwid(user.getCwid());

        Assertions.assertEquals(0, credentials.size());

        Credential cred = credentialService.createNewCredentialForService(user.getCwid(), new CredentialDTO().name("Cred1").apiKey("super_secure").service(CredentialDTO.ServiceEnum.fromValue("canvas")));

        credentials = credentialRepo.getByCwid(user.getCwid());

        Assertions.assertEquals(1, credentials.size());

        Assertions.assertEquals(cred, credentials.getFirst());
    }

    @Test
    void verifyCreateCredentialExists(){
        User user = userSeeder.user1();

        credentialService.createNewCredentialForService(user.getCwid(), new CredentialDTO().name("Cred1").apiKey("super_secure").service(CredentialDTO.ServiceEnum.fromValue("canvas")));

        ResponseStatusException exception = Assertions.assertThrows(ResponseStatusException.class, () -> credentialService.createNewCredentialForService(user.getCwid(), new CredentialDTO().name("Cred1").apiKey("super_secure").service(CredentialDTO.ServiceEnum.fromValue("canvas"))));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        Optional<Credential> credential = credentialRepo.getByCwidAndType(user.getCwid(), CredentialType.CANVAS);

        Assertions.assertTrue(credential.isPresent());
    }

    @Test
    void verifyCreateCredentialSameName(){
        User user = userSeeder.user1();

        credentialService.createNewCredentialForService(user.getCwid(), new CredentialDTO().name("Cred1").apiKey("super_secure").service(CredentialDTO.ServiceEnum.fromValue("canvas")));

        ResponseStatusException exception = Assertions.assertThrows(ResponseStatusException.class, () -> credentialService.createNewCredentialForService(user.getCwid(), new CredentialDTO().name("Cred1").apiKey("super_secure").service(CredentialDTO.ServiceEnum.fromValue("canvas"))));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        List<Credential> credentials = credentialRepo.getByCwid(user.getCwid());

        Assertions.assertEquals(1, credentials.size());
    }

    @Test
    void verifyMarkCredentialAsPublic(){
        User user = userSeeder.user1();

        Credential cred = credentialService.createNewCredentialForService(user.getCwid(), new CredentialDTO().name("Cred1").apiKey("super_secure").service(CredentialDTO.ServiceEnum.fromValue("canvas")));

        Credential newCred = credentialService.markCredentialAsPublic(user.getCwid(), cred.getId());

        Assertions.assertEquals(cred.getId(), newCred.getId());

        Assertions.assertFalse(newCred.isPrivate());
    }

    @Test
    void verifyDeleteCredential(){
        User user = userSeeder.user1();

        Credential cred = credentialService.createNewCredentialForService(user.getCwid(), new CredentialDTO().name("Cred1").apiKey("super_secure").service(CredentialDTO.ServiceEnum.fromValue("canvas")));

        credentialService.deleteCredential(user.getCwid(), cred.getId());

        Assertions.assertEquals(0, credentialRepo.getByCwid(user.getCwid()).size());
    }

    @Test
    void verifyMarkCredentialAsPrivate(){
        User user = userSeeder.user1();

        Credential cred = credentialService.createNewCredentialForService(user.getCwid(), new CredentialDTO().name("Cred1").apiKey("super_secure").service(CredentialDTO.ServiceEnum.fromValue("canvas")));

        Credential newCred = credentialService.markCredentialAsPrivate(user.getCwid(), cred.getId());

        Assertions.assertEquals(cred.getId(), newCred.getId());

        Assertions.assertTrue(newCred.isPrivate());
    }

    @Test
    void verifyGetAllCredentials(){
        User user = userSeeder.user1();

        Credential cred1 = credentialService.createNewCredentialForService(user.getCwid(), new CredentialDTO().name("Cred1").apiKey("super_secure").service(CredentialDTO.ServiceEnum.fromValue("canvas")));

        List<Credential> creds = credentialService.getAllCredentials(user.getCwid());

        Assertions.assertEquals(1, creds.size());
        Assertions.assertTrue(creds.contains(cred1));
    }


}
