package edu.mines.packtrain.services;

import edu.mines.packtrain.events.NewTaskEvent;
import edu.mines.packtrain.managers.IdentityProvider;
import edu.mines.packtrain.managers.ImpersonationManager;
import edu.mines.packtrain.models.Course;
import edu.mines.packtrain.models.CourseMember;
import edu.mines.packtrain.models.Section;
import edu.mines.packtrain.models.User;
import edu.mines.packtrain.models.tasks.ScheduledTaskDef;
import edu.mines.packtrain.models.tasks.SectionSyncTaskDef;
import edu.mines.packtrain.repositories.ScheduledTaskRepo;
import edu.mines.packtrain.repositories.SectionRepo;
import edu.mines.packtrain.services.external.CanvasService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Optional;
import java.util.Set;
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

        Course course = courseService.getCourse(task.getCourseToImport());

        log.debug("Processing {} sections for course '{}'", canvasSections.size(), course.getCode());

        List<Section> sections = canvasSections.stream()
                .map(s -> createSection(s.getId(), s.getName(), course))
                .filter(Optional::isPresent)
                .map(Optional::get)
        .toList();

        log.info("Added {} new sections for course '{}'", sections.size(), course.getCode());
    }

    public Set<Section> getSectionByMember(CourseMember member){
        return sectionRepo.getSectionsByMember(member.getId());
    }

    public Optional<Section> createSection(long canvasId, String name, Course course){
        if (sectionRepo.existsByCanvasId(canvasId)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("Section '%s' already exists, can not create section with same id!", name));
        }

        Section newSection = new Section();
        newSection.setCanvasId(canvasId);
        newSection.setName(name);
        newSection.setCourse(course);

        return Optional.of(sectionRepo.save(newSection));
    }

    public Section getSection(UUID uuid){
        Optional<Section> section = sectionRepo.getById(uuid);
        if (section.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Section '%s' was not found!", uuid));
        }
        return section.get();
    }

    public ScheduledTaskDef createSectionsFromCanvas(SseEmitter emitter, User actingUser, UUID courseId, long canvasId){
        SectionSyncTaskDef task = new SectionSyncTaskDef();
        task.setCreatedByUser(actingUser);
        task.setTaskName(String.format("Sync Course '%s': Course Sections", courseId));
        task.setCourseToImport(courseId);
        task.setCanvasId(canvasId);

        task = taskRepo.save(task);

        NewTaskEvent.TaskData<SectionSyncTaskDef> taskDef = new NewTaskEvent.TaskData<>(taskRepo, task.getId(), this::syncSectionTask, emitter);

        eventPublisher.publishEvent(new NewTaskEvent(this, taskDef));

        return task;
    }

    public List<Section> getSectionsForCourse(UUID courseId){
        return sectionRepo.getSectionsForCourse(courseId);
    }
}
