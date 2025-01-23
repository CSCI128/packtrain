package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.containers.PostgresTestContainer;
import edu.mines.gradingadmin.repositories.CourseRepo;
import edu.mines.gradingadmin.repositories.CredentialRepo;
import edu.mines.gradingadmin.seeders.CourseSeeders;
import edu.mines.gradingadmin.seeders.UserSeeders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@DirtiesContext
class VerifyCredentialService implements PostgresTestContainer {
    public VerifyCredentialService(
            UserSeeders userSeeder,
            CourseSeeders courseSeeders,
            CredentialRepo credentialRepo,
            CredentialService credentialService
    ) {
        this.userSeeder = userSeeder;
        this.courseSeeders = courseSeeders;
        this.credentialRepo = credentialRepo;
        this.credentialService = credentialService;
    }

    final UserSeeders userSeeder;
    final CourseSeeders courseSeeders;
    final CredentialRepo credentialRepo;

    final CredentialService credentialService;

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
        userSeeder.clearAll();
    }


}
