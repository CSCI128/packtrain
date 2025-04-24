package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.containers.PostgresTestContainer;
import edu.mines.gradingadmin.data.CredentialDTO;
import edu.mines.gradingadmin.models.Credential;
import edu.mines.gradingadmin.models.enums.CredentialType;
import edu.mines.gradingadmin.models.User;
import edu.mines.gradingadmin.repositories.CredentialRepo;
import edu.mines.gradingadmin.seeders.CourseSeeders;
import edu.mines.gradingadmin.seeders.UserSeeders;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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

        Optional<Credential> cred = credentialService.createNewCredentialForService(user.getCwid(), new CredentialDTO().name("Cred1").apiKey("super_secure").service(CredentialDTO.ServiceEnum.fromValue("canvas")));

        Assertions.assertTrue(cred.isPresent());

        credentials = credentialRepo.getByCwidAndEndpoint(user.getCwid(), CredentialType.CANVAS);

        Assertions.assertEquals(1, credentials.size());

        Assertions.assertEquals(user.getCwid(), credentials.getFirst().getOwningUser().getCwid());
    }

    @Test
    void verifyCreateCredentialExists(){
        User user = userSeeder.user1();

        credentialService.createNewCredentialForService(user.getCwid(), new CredentialDTO().name("Cred1").apiKey("super_secure").service(CredentialDTO.ServiceEnum.fromValue("canvas")));

        Optional<Credential> cred = credentialService.createNewCredentialForService(user.getCwid(), new CredentialDTO().name("Cred2").apiKey("super_secure").service(CredentialDTO.ServiceEnum.fromValue("canvas")));

        Assertions.assertTrue(cred.isEmpty());

        List<Credential> credentials = credentialRepo.getByCwidAndEndpoint(user.getCwid(), CredentialType.CANVAS);

        Assertions.assertEquals(1, credentials.size());
    }

    @Test
    void verifyCreateCredentialSameName(){
        User user = userSeeder.user1();

        credentialService.createNewCredentialForService(user.getCwid(), new CredentialDTO().name("Cred1").apiKey("super_secure").service(CredentialDTO.ServiceEnum.fromValue("canvas")));

        Optional<Credential> cred = credentialService.createNewCredentialForService(user.getCwid(), new CredentialDTO().name("Cred1").apiKey("super_secure").service(CredentialDTO.ServiceEnum.fromValue("canvas")));

        Assertions.assertTrue(cred.isEmpty());

        List<Credential> credentials = credentialRepo.getByCwid(user.getCwid());

        Assertions.assertEquals(1, credentials.size());
    }

    @Test
    void verifyMarkCredentialAsPublic(){
        User user = userSeeder.user1();

        Credential cred = credentialService.createNewCredentialForService(user.getCwid(), new CredentialDTO().name("Cred1").apiKey("super_secure").service(CredentialDTO.ServiceEnum.fromValue("canvas"))).orElseThrow();

        Credential newCred = credentialService.markCredentialAsPublic(cred.getId()).orElseThrow();

        Assertions.assertEquals(cred.getId(), newCred.getId());

        Assertions.assertFalse(newCred.isPrivate());
    }

    @Test
    void verifyDeleteCredential(){
        User user = userSeeder.user1();

        Credential cred = credentialService.createNewCredentialForService(user.getCwid(), new CredentialDTO().name("Cred1").apiKey("super_secure").service(CredentialDTO.ServiceEnum.fromValue("canvas"))).orElseThrow();

        credentialService.deleteCredential(cred);

        Assertions.assertEquals(0, credentialRepo.getByCwid(user.getCwid()).size());
    }

    @Test
    void verifyMarkCredentialAsPrivate(){
        User user = userSeeder.user1();

        Credential cred = credentialService.createNewCredentialForService(user.getCwid(), new CredentialDTO().name("Cred1").apiKey("super_secure").service(CredentialDTO.ServiceEnum.fromValue("canvas"))).orElseThrow();

        Credential newCred = credentialService.markCredentialAsPublic(cred.getId()).orElseThrow();
        newCred = credentialService.markCredentialAsPrivate(cred.getId()).orElseThrow();

        Assertions.assertEquals(cred.getId(), newCred.getId());

        Assertions.assertTrue(newCred.isPrivate());
    }

    @Test
    void verifyGetAllCredentials(){
        User user = userSeeder.user1();

        Credential cred1 = credentialService.createNewCredentialForService(user.getCwid(), new CredentialDTO().name("Cred1").apiKey("super_secure").service(CredentialDTO.ServiceEnum.fromValue("canvas"))).orElseThrow();

        List<Credential> creds = credentialService.getAllCredentials(user.getCwid());

        Assertions.assertEquals(creds.size(), 1);
        Assertions.assertTrue(creds.contains(cred1));
    }


}
