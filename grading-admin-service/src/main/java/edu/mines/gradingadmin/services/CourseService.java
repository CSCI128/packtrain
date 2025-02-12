package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.events.NewTaskEvent;
import edu.mines.gradingadmin.managers.IdentityProvider;
import edu.mines.gradingadmin.managers.ImpersonationManager;
import edu.mines.gradingadmin.models.*;
import edu.mines.gradingadmin.models.tasks.CourseImportTaskDef;
import edu.mines.gradingadmin.models.tasks.ScheduledTaskDef;
import edu.mines.gradingadmin.models.tasks.SectionImportTaskDef;
import edu.mines.gradingadmin.repositories.CourseRepo;
import edu.mines.gradingadmin.repositories.ScheduledTaskRepo;
import lombok.extern.slf4j.Slf4j;
import jakarta.transaction.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@Transactional
public class CourseService {
    private final CourseRepo courseRepo;
    private final ScheduledTaskRepo<CourseImportTaskDef> taskRepo;

    private final ApplicationEventPublisher eventPublisher;
    private final ImpersonationManager impersonationManager;
    private final CanvasService canvasService;

    public CourseService(CourseRepo courseRepo, ScheduledTaskRepo<CourseImportTaskDef> taskRepo,
                         ApplicationEventPublisher eventPublisher, ImpersonationManager impersonationManager, CanvasService canvasService) {
        this.courseRepo = courseRepo;
        this.taskRepo = taskRepo;
        this.impersonationManager = impersonationManager;
        this.canvasService = canvasService;
        this.eventPublisher = eventPublisher;
    }

    public List<Course> getCourses(boolean enabled) {
        if (enabled) {
            return courseRepo.getAll(enabled);
        }
        return courseRepo.getAll();
    }

    public Optional<Course> getCourse(UUID courseId){
        return courseRepo.getById(courseId);
    }

    public void syncCourseTask(CourseImportTaskDef task){
        IdentityProvider user = impersonationManager.impersonateUser(task.getCreatedByUser());
        List<edu.ksu.canvas.model.Course> availableCourses =
                canvasService.asUser(user).getAllAvailableCourses()
                        .stream()
                        .filter(course -> course.getId() == task.getCanvasId())
                        .toList();

        if (availableCourses.isEmpty()) {
            log.warn("No courses were found with canvas id '{}'", task.getCanvasId());
            throw new RuntimeException(String.format("Failed to find Canvas course: '%d'", task.getCanvasId()));
        }

        if (availableCourses.size() != 1) {
            log.warn("More than one course was found with canvas id '{}'. This shouldn't be possible", task.getCanvasId());
            throw new RuntimeException(String.format("Failed to find Canvas course: '%d'", task.getCanvasId()));
        }

        edu.ksu.canvas.model.Course canvasCourse = availableCourses.getFirst();

        Optional<Course> toUpdate = courseRepo.getById(task.getCourseToImport());

        if (toUpdate.isEmpty()){
            log.warn("Course to sync not found!");
            throw new RuntimeException(String.format("Failed to find course: '%s'", task.getCourseToImport()));
        }

        toUpdate.get().setCanvasId(canvasCourse.getId());
        if (task.isOverwriteCode())
            toUpdate.get().setCode(canvasCourse.getCourseCode());
        if (task.isOverwriteName())
            toUpdate.get().setName(canvasCourse.getName());

        courseRepo.save(toUpdate.get());
    }

    public Optional<ScheduledTaskDef> importCourseFromCanvas(User actingUser, UUID courseId, long canvasId,
                                                             boolean overwriteName, boolean overwriteCode) {
        if (!courseRepo.existsById(courseId)){
            log.warn("Course '{}' has not been created!", courseId);
            return Optional.empty();
        }

        if (courseRepo.existsByCanvasId(canvasId)) {
            log.warn("Canvas course '{}' has already been imported!", canvasId);
            return Optional.empty();
        }

        CourseImportTaskDef task = new CourseImportTaskDef();
        task.setCreatedByUser(actingUser);
        task.setCourseToImport(courseId);
        task.setCanvasId(canvasId);
        task.setOverwriteName(overwriteName);
        task.setOverwriteCode(overwriteCode);

        task = taskRepo.save(task);

        NewTaskEvent.TaskData<CourseImportTaskDef> taskDef = new NewTaskEvent.TaskData<>(taskRepo, task.getId(), this::syncCourseTask);

        eventPublisher.publishEvent(new NewTaskEvent(this, taskDef));

        return Optional.of(task);
    }



    public void enableCourse(UUID courseId) {
        Optional<Course> course = courseRepo.findById(courseId);
        if(course.isPresent()) {
            course.get().setEnabled(true);
            courseRepo.save(course.get());
        }
    }

    public void disableCourse(UUID courseId) {
        Optional<Course> course = courseRepo.findById(courseId);
        if(course.isPresent()) {
            course.get().setEnabled(false);
            courseRepo.save(course.get());
        }
    }

    public Optional<Course> createNewCourse(String name, String term, String courseCode){
        Course newCourse = new Course();
        newCourse.setName(name);
        newCourse.setCode(courseCode);
        newCourse.setTerm(term);
        newCourse.setEnabled(true);
        newCourse = courseRepo.save(newCourse);
        return Optional.of(newCourse);
    }
}
