package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.containers.PostgresTestContainer;
import edu.mines.gradingadmin.events.NewTaskEvent;
import edu.mines.gradingadmin.models.tasks.ScheduleStatus;
import edu.mines.gradingadmin.models.tasks.ScheduledTaskDef;
import edu.mines.gradingadmin.repositories.ScheduledTaskRepo;
import edu.mines.gradingadmin.seeders.UserSeeders;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Entity(name = "test_task")
@Table(name = "test_tasks")
@EqualsAndHashCode(callSuper = true)
@Data
class TestTaskDef extends ScheduledTaskDef {
}

@Repository
interface TestTaskRepo extends ScheduledTaskRepo<TestTaskDef> {
}

@SpringBootTest
public class TestTaskExecutorService implements PostgresTestContainer {
    @Autowired
    private TestTaskRepo testTaskRepo;

    @Autowired
    private TaskExecutorService executorService;

    @Autowired
    private UserSeeders userSeeders;

    @BeforeAll
    static void setupClass() {
        postgres.start();
    }

    @AfterEach
    void tearDown() {
        testTaskRepo.deleteAll();
        userSeeders.clearAll();
    }

    @Test
    void verifyRepoStatusUpdate(){
        var admin = userSeeders.admin1();
        var task = new TestTaskDef();

        task.setCreatedByUser(admin);
        task.setTaskName("Test Task");

        task = testTaskRepo.save(task);

        testTaskRepo.setStatus(task.getId(), ScheduleStatus.COMPLETED);

        task = testTaskRepo.getById(task.getId()).orElseThrow(AssertionError::new);

        Assertions.assertEquals(ScheduleStatus.COMPLETED, task.getStatus());
    }

    @Test
    void verifyTaskRuns() {
        var admin = userSeeders.admin1();
        var task = new TestTaskDef();

        task.setCreatedByUser(admin);
        task.setTaskName("Test Task");

        task = testTaskRepo.save(task);

        Assertions.assertEquals(ScheduleStatus.CREATED, task.getStatus());

        var data = new NewTaskEvent.TaskData<>(testTaskRepo, task.getId(), _ -> {});

        TaskExecutorService.runTask(data);

        task = testTaskRepo.getById(task.getId()).orElseThrow(AssertionError::new);


        Assertions.assertEquals(ScheduleStatus.COMPLETED, task.getStatus());
        Assertions.assertNotNull(task.getCompletedTime());
    }

    @Test
    void verifyTaskRunsIfDepPass(){
        var admin = userSeeders.admin1();
        var depTask = new TestTaskDef();
        depTask.setCreatedByUser(admin);
        depTask.setTaskName("Test Task");
        depTask.setStatus(ScheduleStatus.COMPLETED);

        depTask = testTaskRepo.save(depTask);

        var task = new TestTaskDef();

        task.setCreatedByUser(admin);
        task.setTaskName("Test Task");

        task = testTaskRepo.save(task);

        var data = new NewTaskEvent.TaskData<>(testTaskRepo, task.getId(), _ -> {});
        data.setDependsOn(Set.of(depTask.getId()));

        TaskExecutorService.runTask(data);

        task = testTaskRepo.getById(task.getId()).orElseThrow(AssertionError::new);


        Assertions.assertEquals(ScheduleStatus.COMPLETED, task.getStatus());
    }

    @Test
    void verifyTaskFailsIfDepFails(){
        var admin = userSeeders.admin1();
        var failedTask = new TestTaskDef();
        failedTask.setCreatedByUser(admin);
        failedTask.setTaskName("Test Task");
        failedTask.setStatus(ScheduleStatus.FAILED);

        failedTask = testTaskRepo.save(failedTask);

        var task = new TestTaskDef();

        task.setCreatedByUser(admin);
        task.setTaskName("Test Task");

        task = testTaskRepo.save(task);

        var data = new NewTaskEvent.TaskData<>(testTaskRepo, task.getId(), _ -> {});
        data.setDependsOn(Set.of(failedTask.getId()));

        TaskExecutorService.runTask(data);

        task = testTaskRepo.getById(task.getId()).orElseThrow(AssertionError::new);


        Assertions.assertEquals(ScheduleStatus.FAILED, task.getStatus());
        Assertions.assertNull(task.getCompletedTime());
    }

    @Test
    void verifyTaskFailsIfException(){
        var admin = userSeeders.admin1();
        var task = new TestTaskDef();
        task.setTaskName("Test Task");

        task.setCreatedByUser(admin);

        task = testTaskRepo.save(task);

        Assertions.assertEquals(ScheduleStatus.CREATED, task.getStatus());

        var expected = "huzzah";

        var data = new NewTaskEvent.TaskData<>(testTaskRepo, task.getId(), _ -> {
            throw new RuntimeException(expected);
        });

        TaskExecutorService.runTask(data);

        task = testTaskRepo.getById(task.getId()).orElseThrow(AssertionError::new);


        Assertions.assertEquals(ScheduleStatus.FAILED, task.getStatus());
        Assertions.assertTrue(task.getStatusText().contains(expected));
    }

    @Test
    void verifyOnlyGetTasksForUser(){
        var user1 = userSeeders.user1();
        var user2 = userSeeders.user2();

        var taskForUser1 = new TestTaskDef();

        taskForUser1.setCreatedByUser(user1);
        taskForUser1.setTaskName("Test Task");

        taskForUser1 = testTaskRepo.save(taskForUser1);

        var taskForUser2 = new TestTaskDef();

        taskForUser2.setCreatedByUser(user2);
        taskForUser2.setTaskName("Test Task");

        taskForUser2 = testTaskRepo.save(taskForUser2);

        List<ScheduledTaskDef> tasks = executorService.getScheduledTasks(user1);

        Assertions.assertEquals(1, tasks.size());
        Assertions.assertEquals(taskForUser1.getId(), tasks.getFirst().getId());
    }

    @Test
    void verifyRefuseToGetTaskForOtherUser(){
        var user1 = userSeeders.user1();
        var user2 = userSeeders.user2();

        var taskForUser1 = new TestTaskDef();

        taskForUser1.setTaskName("Test Task");
        taskForUser1.setCreatedByUser(user1);

        taskForUser1 = testTaskRepo.save(taskForUser1);

        var taskForUser2 = new TestTaskDef();

        taskForUser2.setCreatedByUser(user2);
        taskForUser2.setTaskName("Test Task");

        taskForUser2 = testTaskRepo.save(taskForUser2);

        TestTaskDef finalTaskForUser = taskForUser2;
        ResponseStatusException exception = Assertions.assertThrows(ResponseStatusException.class, () -> executorService.getScheduledTask(user1, finalTaskForUser.getId()));
        Assertions.assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        Assertions.assertEquals("Task does not exist", exception.getReason());
    }

    @Test
    void verifyGetTaskForUser(){
        var user1 = userSeeders.user1();
        var user2 = userSeeders.user2();

        var taskForUser1 = new TestTaskDef();
        taskForUser1.setTaskName("Test Task");

        taskForUser1.setCreatedByUser(user1);

        taskForUser1 = testTaskRepo.save(taskForUser1);

        var taskForUser2 = new TestTaskDef();

        taskForUser2.setCreatedByUser(user2);
        taskForUser2.setTaskName("Test Task");

        taskForUser2 = testTaskRepo.save(taskForUser2);

        Optional<ScheduledTaskDef> actualTasks = executorService.getScheduledTask(user1, taskForUser1.getId());
        Assertions.assertTrue(actualTasks.isPresent());
    }

}
