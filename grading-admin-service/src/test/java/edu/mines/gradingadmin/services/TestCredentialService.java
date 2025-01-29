package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.containers.PostgresTestContainer;
import edu.mines.gradingadmin.models.Credential;
import edu.mines.gradingadmin.models.ExternalSource;
import edu.mines.gradingadmin.models.User;
import edu.mines.gradingadmin.repositories.CredentialRepo;
import edu.mines.gradingadmin.seeders.CourseSeeders;
import edu.mines.gradingadmin.seeders.ExternalSourceSeeders;
import edu.mines.gradingadmin.seeders.UserSeeders;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.Optional;

@SpringBootTest
@DirtiesContext
@Transactional
class TestCredentialService implements PostgresTestContainer {
    @Autowired
    UserSeeders userSeeder;
    @Autowired
    CourseSeeders courseSeeders;
    @Autowired
    ExternalSourceSeeders externalSourceSeeders;
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
        externalSourceSeeders.clearAll();
    }


    @Test
    void verifyCreateNewCredentialDoesNotExist(){
        User user = userSeeder.user1();
        ExternalSource externalSource = externalSourceSeeders.externalSource1();

        List<Credential> credentials = credentialRepo.getAllByCwid(user.getCwid());

        Assertions.assertEquals(0, credentials.size());

        Optional<Credential> cred = credentialService.createNewCredentialForService(user.getCwid(), "Cred1", "super_secure", externalSource.getEndpoint());

        Assertions.assertTrue(cred.isPresent());

        credentials = credentialRepo.getByCwidAndEndpoint(user.getCwid(), externalSource.getEndpoint());

        Assertions.assertEquals(1, credentials.size());

        Assertions.assertEquals(user.getCwid(), credentials.getFirst().getOwningUser().getCwid());
    }

    @Test
    void verifyCreateCredentialExists(){
        User user = userSeeder.user1();
        ExternalSource externalSource = externalSourceSeeders.externalSource1();

        credentialService.createNewCredentialForService(user.getCwid(), "Cred1", "super_secure", externalSource.getEndpoint());

        Optional<Credential> cred = credentialService.createNewCredentialForService(user.getCwid(), "Cred2", "super_secure", externalSource.getEndpoint());

        Assertions.assertTrue(cred.isEmpty());

        List<Credential> credentials = credentialRepo.getByCwidAndEndpoint(user.getCwid(), externalSource.getEndpoint());

        Assertions.assertEquals(1, credentials.size());
    }

    @Test
    void verifyCreateCredentialSameName(){
        User user = userSeeder.user1();
        ExternalSource externalSource1 = externalSourceSeeders.externalSource1();
        ExternalSource externalSource2 = externalSourceSeeders.externalSource2();

        credentialService.createNewCredentialForService(user.getCwid(), "Cred1", "super_secure", externalSource1.getEndpoint());

        Optional<Credential> cred = credentialService.createNewCredentialForService(user.getCwid(), "Cred1", "super_secure", externalSource2.getEndpoint());

        Assertions.assertTrue(cred.isEmpty());

        List<Credential> credentials = credentialRepo.getByCwid(user.getCwid());

        Assertions.assertEquals(1, credentials.size());
    }

    @Test
    void verifyCreateCredentialSameNameInactive(){
        User user = userSeeder.user1();
        ExternalSource externalSource1 = externalSourceSeeders.externalSource1();
        ExternalSource externalSource2 = externalSourceSeeders.externalSource2();

        Optional<Credential> cred = credentialService.createNewCredentialForService(user.getCwid(), "Cred1", "super_secure", externalSource1.getEndpoint());

        Assertions.assertFalse(cred.isEmpty());

        credentialService.markCredentialAsInactive(cred.get().getId());

        cred = credentialService.createNewCredentialForService(user.getCwid(), "Cred1", "super_secure", externalSource2.getEndpoint());

        Assertions.assertFalse(cred.isEmpty());

        List<Credential> credentials = credentialRepo.getByCwid(user.getCwid());

        Assertions.assertEquals(1, credentials.size());
    }

    @Test
    void verifyMarkCredentialAsPublic(){
        User user = userSeeder.user1();
        ExternalSource externalSource1 = externalSourceSeeders.externalSource1();

        Credential cred = credentialService.createNewCredentialForService(user.getCwid(), "Cred1", "super_secure", externalSource1.getEndpoint()).orElseThrow();

        Credential newCred = credentialService.markCredentialAsPublic(cred.getId()).orElseThrow();

        Assertions.assertEquals(cred.getId(), newCred.getId());

        Assertions.assertFalse(newCred.isPrivate());
    }

    @Test
    void verifyMarkCredentialAsPublicInactive(){
        User user = userSeeder.user1();
        ExternalSource externalSource1 = externalSourceSeeders.externalSource1();

        Credential cred = credentialService.createNewCredentialForService(user.getCwid(), "Cred1", "super_secure", externalSource1.getEndpoint()).orElseThrow();

        credentialService.markCredentialAsInactive(cred.getId());

        Optional<Credential> newCred = credentialService.markCredentialAsPublic(cred.getId());

        Assertions.assertTrue(newCred.isEmpty());
    }

    @Test
    void verifyDisableCredential(){
        User user = userSeeder.user1();
        ExternalSource externalSource1 = externalSourceSeeders.externalSource1();

        Credential cred = credentialService.createNewCredentialForService(user.getCwid(), "Cred1", "super_secure", externalSource1.getEndpoint()).orElseThrow();

        Credential newCred = credentialService.markCredentialAsInactive(cred.getId()).orElseThrow();

        Assertions.assertEquals(cred.getId(), newCred.getId());

        Assertions.assertFalse(newCred.isActive());

        Assertions.assertEquals(0, credentialRepo.getByCwid(user.getCwid()).size());
        Assertions.assertEquals(1, credentialRepo.getAllByCwid(user.getCwid()).size());


    }
}
