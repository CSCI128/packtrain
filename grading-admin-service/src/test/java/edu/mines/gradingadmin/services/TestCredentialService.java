package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.containers.PostgresTestContainer;
import edu.mines.gradingadmin.models.Credential;
import edu.mines.gradingadmin.models.ExternalSource;
import edu.mines.gradingadmin.models.User;
import edu.mines.gradingadmin.repositories.CredentialRepo;
import edu.mines.gradingadmin.seeders.CourseSeeders;
import edu.mines.gradingadmin.seeders.ExternalSourceSeeders;
import edu.mines.gradingadmin.seeders.UserSeeders;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

// for some *fun* reason, spring refuses to load all the services requested unless this is added.
// great job spring developers.
// thanks for making your docs crystal clear
@SpringBootTest
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

    @AfterAll
    static void tearDownClass() {
        postgres.stop();
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

        credentialService.createNewCredentialForService(user.getCwid(), "Cred1", "super_secure", externalSource.getEndpoint());

        List<Credential> credentials = credentialRepo.getByCwidAndEndpoint(user.getCwid(), externalSource.getEndpoint());

        Assertions.assertEquals(1, credentials.size());

        Assertions.assertEquals(user.getId(), credentials.getFirst().getOwningUser().getId());
    }


}
