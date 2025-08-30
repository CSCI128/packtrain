package edu.mines.packtrain.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.mines.packtrain.data.CourseSyncDTO;
import edu.mines.packtrain.factories.DTOFactory;
import edu.mines.packtrain.managers.ImpersonationManager;
import edu.mines.packtrain.managers.SecurityManager;
import edu.mines.packtrain.models.User;
import edu.mines.packtrain.models.tasks.ScheduledTaskDef;
import edu.mines.packtrain.services.*;
import edu.mines.packtrain.services.tasks.AssignmentTaskService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Controller
@RequiredArgsConstructor
public class NotificationController {

    private final CourseService courseService;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;
    private final SectionService sectionService;
    private final CourseMemberService courseMemberService;
    private final AssignmentTaskService assignmentTaskService;

    @MessageMapping("/importCourse") // app/importCourse is the "endpoint"
    @SendTo("/courses/import") // response being sent; frontend "endpoint"
    public String importCourse(String message, Principal principal) {
        User user = userService.getUserByCwid(principal.getName());

        try {
            CourseSyncDTO courseSyncDTO = objectMapper.readValue(message, CourseSyncDTO.class);
            UUID courseId = UUID.fromString(courseSyncDTO.getCourseId());
            long canvasId = Long.parseLong(courseSyncDTO.getCanvasId());

            ScheduledTaskDef courseTask = courseService.syncCourseWithCanvas(
                user, courseId, canvasId,
                false, false, messagingTemplate);

            ScheduledTaskDef sectionTask = sectionService.createSectionsFromCanvas(
                    user, courseId, canvasId, messagingTemplate);

            courseMemberService.syncMembersFromCanvas(user, Set.of(courseTask.getId(), sectionTask.getId()), courseId, true, true, true, messagingTemplate);

            assignmentTaskService.syncAssignmentsFromCanvas(user, Set.of(courseTask.getId()), courseId, true, true, true, messagingTemplate);
        } catch (JsonProcessingException e) {
            // TODO handle this error and send back better response; error DTO?
            throw new RuntimeException(e);
        }

        log.warn("Received: " + message);
        return "ACKNOWLEDGED IMPORT COURSE: " + message;
    }
}
