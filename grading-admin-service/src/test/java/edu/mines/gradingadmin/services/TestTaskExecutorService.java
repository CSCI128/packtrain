package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.containers.PostgresTestContainer;
import edu.mines.gradingadmin.models.ScheduledTaskDef;
import edu.mines.gradingadmin.repositories.ScheduledTaskRepo;
import edu.mines.gradingadmin.seeders.UserSeeders;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Repository;

@Entity(name = "test_task")
@Table(name = "test_tasks")
class TestTaskDef extends ScheduledTaskDef{ }

@Repository
interface TestTaskRepo extends ScheduledTaskRepo<TestTaskDef>{}

@SpringBootTest
public class TestTaskExecutorService implements PostgresTestContainer {
    @Autowired
    private TestTaskRepo testTaskRepo;

    @Autowired
    private TaskExecutorService executorService;

    @Autowired
    private UserSeeders userSeeders;

    @BeforeAll
    static void setupClass(){
        postgres.start();
    }

    @AfterEach
    void tearDown(){
        testTaskRepo.deleteAll();
        userSeeders.clearAll();
    }

    @Test
    void verifyTaskExecutesWhenEventEmitted(){
        var admin = userSeeders.admin1();
        var task = new TestTaskDef();

        task.setCreatedByUser(admin);

        task = testTaskRepo.save(task);



    }


}
