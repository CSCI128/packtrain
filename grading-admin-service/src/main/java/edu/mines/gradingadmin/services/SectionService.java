package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.events.NewTaskEvent;
import edu.mines.gradingadmin.managers.IdentityProvider;
import edu.mines.gradingadmin.managers.ImpersonationManager;
import edu.mines.gradingadmin.models.Course;
import edu.mines.gradingadmin.models.Section;
import edu.mines.gradingadmin.models.User;
import edu.mines.gradingadmin.models.tasks.ScheduledTaskDef;
import edu.mines.gradingadmin.models.tasks.SectionImportTaskDef;
import edu.mines.gradingadmin.repositories.ScheduledTaskRepo;
import edu.mines.gradingadmin.repositories.SectionRepo;
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
    private final ScheduledTaskRepo<SectionImportTaskDef> taskRepo;
    private final CourseService courseService;
    private final CanvasService canvasService;

    private final ApplicationEventPublisher eventPublisher;
    private final ImpersonationManager impersonationManager;

    public SectionService(SectionRepo sectionRepo, ScheduledTaskRepo<SectionImportTaskDef> taskRepo, CourseService courseService, CanvasService canvasService, ApplicationEventPublisher eventPublisher, ImpersonationManager impersonationManager) {
        this.sectionRepo = sectionRepo;
        this.taskRepo = taskRepo;
        this.courseService = courseService;
        this.canvasService = canvasService;
        this.eventPublisher = eventPublisher;
        this.impersonationManager = impersonationManager;
    }

    public void syncSectionTask(SectionImportTaskDef task){
        IdentityProvider user = impersonationManager.impersonateUser(task.getCreatedByUser());
        List<edu.ksu.canvas.model.Section> canvasSections = canvasService.asUser(user).getCourseSections(task.getCanvasId())
                .stream().filter(section -> !sectionRepo.existsByCanvasId(section.getId())).toList();

        Optional<Course> course = courseService.getCourse(task.getCourseToImport());

        if (course.isEmpty()){
            log.warn("Requested course does not exit");
            return;
        }

        log.info("Adding {} new sections for course '{}'", canvasSections.size(), course.get().getCode());

        List<Section> sections = canvasSections.stream().map(section -> {
            var newSection = new Section();
            newSection.setCanvasId(section.getId());
            newSection.setName(section.getName());
            newSection.setCourse(course.get());
            return newSection;
        }).toList();

        sectionRepo.saveAll(sections);
    }

    public Optional<ScheduledTaskDef> createSectionsFromCanvas(User actingUser, UUID courseId, long canvasId){
        SectionImportTaskDef task = new SectionImportTaskDef();
        task.setCreatedByUser(actingUser);
        task.setCourseToImport(courseId);
        task.setCanvasId(canvasId);

        task = taskRepo.save(task);

        NewTaskEvent.TaskData<SectionImportTaskDef> taskDef = new NewTaskEvent.TaskData<>(taskRepo, task.getId(), this::syncSectionTask);

        eventPublisher.publishEvent(new NewTaskEvent(this, taskDef));

        return Optional.of(task);
    }

    public List<Section> getSectionsForCourse(UUID courseId){
        return sectionRepo.getSectionsForCourse(courseId);
    }
}
