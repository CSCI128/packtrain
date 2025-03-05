package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.events.NewTaskEvent;
import edu.mines.gradingadmin.managers.IdentityProvider;
import edu.mines.gradingadmin.managers.ImpersonationManager;
import edu.mines.gradingadmin.models.Course;
import edu.mines.gradingadmin.models.Section;
import edu.mines.gradingadmin.models.User;
import edu.mines.gradingadmin.models.tasks.ScheduledTaskDef;
import edu.mines.gradingadmin.models.tasks.SectionSyncTaskDef;
import edu.mines.gradingadmin.repositories.ScheduledTaskRepo;
import edu.mines.gradingadmin.repositories.SectionRepo;
import edu.mines.gradingadmin.services.external.CanvasService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class SectionService {
    private final SectionRepo sectionRepo;
    private final ScheduledTaskRepo<SectionSyncTaskDef> taskRepo;
    private final CourseService courseService;
    private final CanvasService canvasService;

    private final ApplicationEventPublisher eventPublisher;
    private final ImpersonationManager impersonationManager;

    public SectionService(SectionRepo sectionRepo, ScheduledTaskRepo<SectionSyncTaskDef> taskRepo, CourseService courseService, CanvasService canvasService, ApplicationEventPublisher eventPublisher, ImpersonationManager impersonationManager) {
        this.sectionRepo = sectionRepo;
        this.taskRepo = taskRepo;
        this.courseService = courseService;
        this.canvasService = canvasService;
        this.eventPublisher = eventPublisher;
        this.impersonationManager = impersonationManager;
    }

    public void syncSectionTask(SectionSyncTaskDef task){
        IdentityProvider user = impersonationManager.impersonateUser(task.getCreatedByUser());
        List<edu.ksu.canvas.model.Section> canvasSections = canvasService.asUser(user).getCourseSections(task.getCanvasId());

        Optional<Course> course = courseService.getCourse(task.getCourseToImport());

        if (course.isEmpty()){
            log.warn("Requested course does not exit");
            return;
        }

        log.debug("Processing {} sections for course '{}'", canvasSections.size(), course.get().getCode());

        List<Section> sections = canvasSections.stream()
                .map(s -> createSection(s.getId(), s.getName(), course.get()))
                .filter(Optional::isPresent)
                .map(Optional::get)
        .toList();

        log.info("Added {} new sections for course '{}'", sections.size(), course.get().getCode());
    }

    public Optional<Section> createSection(long canvasId, String name, Course course){
        if (sectionRepo.existsByCanvasId(canvasId)){
            return Optional.empty();
        }

        Section newSection = new Section();
        newSection.setCanvasId(canvasId);
        newSection.setName(name);
        newSection.setCourse(course);

        return Optional.of(sectionRepo.save(newSection));
    }

    public Optional<Section> getSection(UUID uuid){
        return sectionRepo.getById(uuid);
    }

    public Optional<ScheduledTaskDef> createSectionsFromCanvas(User actingUser, UUID courseId, long canvasId){
        SectionSyncTaskDef task = new SectionSyncTaskDef();
        task.setCreatedByUser(actingUser);
        task.setCourseToImport(courseId);
        task.setCanvasId(canvasId);

        task = taskRepo.save(task);

        NewTaskEvent.TaskData<SectionSyncTaskDef> taskDef = new NewTaskEvent.TaskData<>(taskRepo, task.getId(), this::syncSectionTask);

        eventPublisher.publishEvent(new NewTaskEvent(this, taskDef));

        return Optional.of(task);
    }

    public List<Section> getSectionsForCourse(UUID courseId){
        return sectionRepo.getSectionsForCourse(courseId);
    }
}
