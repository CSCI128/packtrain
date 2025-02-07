package edu.mines.gradingadmin.repositories;

import edu.mines.gradingadmin.models.ScheduleStatus;
import edu.mines.gradingadmin.models.ScheduledTaskDef;
import edu.mines.gradingadmin.models.User;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface ScheduledTaskRepo<T extends ScheduledTaskDef> extends CrudRepository<T, Long> {
    @Query("select e from #{#entityName} e where e.id = ?1")
    Optional<T> getById(long id);

    @Query("select e from #{#entityName} e where e.createdByUser = ?1 order by e.submittedTime")
    List<T> getTasksForUser(User user);

    @Query("select e.status from #{#entityName} e where e.id = ?1")
    Optional<ScheduleStatus> getStatus(long id);

    @Modifying
    @Query("update #{#entityName} e set e.status = ?2, e.statusText = ?3 where e.id = ?1")
    void setStatus(long id, ScheduleStatus status, String statusText);

    @Modifying
    @Query("update #{#entityName} e set e.status = ?2 where e.id = ?1")
    void setStatus(long id, ScheduleStatus status);

    @Modifying
    @Query("update #{#entityName} e set e.completedTime = ?2 where e.id = ?1")
    void setCompletedTime(long id, Instant completedTime);

}
