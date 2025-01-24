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
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;

@SpringBootTest
@DirtiesContext
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

        List<Credential> credentials = credentialRepo.getByCwidAndEndpoint(user.getCwid(), externalSource.getEndpoint());

        Assertions.assertEquals(0, credentials.size());


        credentialService.createNewCredentialForService(user.getCwid(), "Cred1", "super_secure", externalSource.getEndpoint());

        credentials = credentialRepo.getByCwidAndEndpoint(user.getCwid(), externalSource.getEndpoint());

        Assertions.assertEquals(1, credentials.size());

        Assertions.assertEquals(user.getId(), credentials.getFirst().getOwningUser().getId());
    }


}
