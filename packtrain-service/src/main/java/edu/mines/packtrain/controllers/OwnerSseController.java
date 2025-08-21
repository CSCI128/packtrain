package edu.mines.packtrain.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.mines.packtrain.factories.DTOFactory;
import edu.mines.packtrain.models.User;
import edu.mines.packtrain.models.tasks.ScheduledTaskDef;
import edu.mines.packtrain.services.*;
import edu.mines.packtrain.managers.SecurityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import edu.mines.packtrain.services.tasks.AssignmentTaskService;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/owner/courses")
public class OwnerSseController {

    private         ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);


    private final CourseService courseService;
    private final SectionService sectionService;
    private final CourseMemberService courseMemberService;
    private final AssignmentTaskService assignmentTaskService;
    private final SecurityManager securityManager;
    private final ObjectMapper objectMapper;

    @GetMapping(path = "/{courseId}/import", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter importCourse(
            @PathVariable UUID courseId,
            @RequestParam(name = "canvas_id") Long canvasId,
            @RequestParam(name = "overwrite_name", defaultValue = "false") boolean overwriteName,
            @RequestParam(name = "overwrite_code", defaultValue = "false") boolean overwriteCode,
            @RequestParam(name = "import_users", defaultValue = "false") boolean importUsers,
            @RequestParam(name = "import_assignments", defaultValue = "false") boolean importAssignments
    ) {
        SseEmitter emitter = new SseEmitter();

        scheduler.scheduleAtFixedRate(() -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("ping")
                        .data("keep-alive"));
            } catch (IOException ex) {
                emitter.completeWithError(ex);
            }
        }, 0, 30, TimeUnit.SECONDS);

        // TODO error handle if gradescopeconfig already exists
        // TODO re-add headers with settings

        User currentUser = securityManager.getUser();
        CompletableFuture.runAsync(() -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("status")
                        .data("Task started for course " + courseId));

                ScheduledTaskDef course = courseService.syncCourseWithCanvas(emitter,
                        currentUser, courseId, canvasId,
                        overwriteName, overwriteCode);

                emitter.send(SseEmitter.event()
                        .name("status")
                        .data("creating sections from canvas"));

                ScheduledTaskDef sections = sectionService.createSectionsFromCanvas(emitter,
                        currentUser, courseId, canvasId);

                emitter.send(SseEmitter.event()
                        .name("status")
                        .data("creating users from canvas"));

////        if (courseSyncTaskDTO.getImportUsers()) {
                courseMemberService.syncMembersFromCanvas(emitter, currentUser, Set.of(course.getId(), sections.getId()), courseId, true, true, true);
////        }

                emitter.send(SseEmitter.event()
                        .name("status")
                        .data("creating assignments from canvas"));

////        if (courseSyncTaskDTO.getImportAssignments()) {
                assignmentTaskService.syncAssignmentsFromCanvas(emitter, currentUser, Set.of(course.getId()), courseId, true, true, true);
////        }

                emitter.send(SseEmitter.event()
                        .name("result")
                        .data("Task finished for course " + objectMapper.writeValueAsString(DTOFactory.toDto(course))));

//                emitter.send(SseEmitter.event()
//                        .name("end")
//                        .data("successful end of stream"));
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
   }
}
