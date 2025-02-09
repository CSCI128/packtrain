package edu.mines.gradingadmin.controllers;

import edu.mines.gradingadmin.api.AdminApiDelegate;
import edu.mines.gradingadmin.data.*;
import edu.mines.gradingadmin.managers.SecurityManager;
import edu.mines.gradingadmin.models.Course;
import edu.mines.gradingadmin.models.Section;
import edu.mines.gradingadmin.models.tasks.ScheduledTaskDef;
import edu.mines.gradingadmin.services.CourseMemberService;
import edu.mines.gradingadmin.services.CourseService;
import edu.mines.gradingadmin.services.SectionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import javax.swing.text.html.Option;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
public class AdminApiImpl implements AdminApiDelegate {

    private final CourseService courseService;
    private final SectionService sectionService;
    private final CourseMemberService courseMemberService;
    private final SecurityManager securityManager;

    public AdminApiImpl(CourseService courseService, SectionService sectionService, CourseMemberService courseMemberService, SecurityManager securityManager) {
        this.courseService = courseService;
        this.sectionService = sectionService;
        this.courseMemberService = courseMemberService;
        this.securityManager = securityManager;
    }

    @Override
    public ResponseEntity<List<TaskDTO>> importCourse(String courseId, CourseSyncTaskDTO courseSyncTaskDTO) {
        List<TaskDTO> tasks = new LinkedList<>();
        UUID courseUUID = UUID.fromString(courseId);

        Optional<ScheduledTaskDef> courseTask = courseService.importCourseFromCanvas(
                securityManager.getUser(), courseUUID, courseSyncTaskDTO.getCanvasId(),
                courseSyncTaskDTO.getOverwriteName(), courseSyncTaskDTO.getOverwriteCode());

        if (courseTask.isEmpty()){
            return ResponseEntity.badRequest().build();
        }

        tasks.add(courseTask.map(t -> new TaskDTO().id(t.getId()).status(t.getStatus().toString()).submittedTime(t.getSubmittedTime())).get());

        Optional<ScheduledTaskDef> sectionTask = sectionService.createSectionsFromCanvas(
                securityManager.getUser(), courseUUID, courseSyncTaskDTO.getCanvasId());


        if (sectionTask.isEmpty()){
            return ResponseEntity.badRequest().build();
        }

        tasks.add(sectionTask.map(t -> new TaskDTO().id(t.getId()).status(t.getStatus().toString()).submittedTime(t.getSubmittedTime())).get());

        if (courseSyncTaskDTO.getImportUsers()){
            Optional<ScheduledTaskDef> importUsersTask = courseMemberService.addMembersToCourse(securityManager.getUser(), Set.of(courseTask.get().getId(), sectionTask.get().getId()), courseUUID);

            if (importUsersTask.isEmpty()){
                return ResponseEntity.badRequest().build();
            }

            tasks.add(importUsersTask.map(t -> new TaskDTO().id(t.getId()).status(t.getStatus().toString()).submittedTime(t.getSubmittedTime())).get());
        }

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(tasks);
    }

    @Override
    public ResponseEntity<CourseDTO> newCourse(CourseDTO courseDTO) {
        Optional<Course> course = courseService.createNewCourse(courseDTO.getName(), courseDTO.getTerm(), courseDTO.getCode());
        if (course.isEmpty()){
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(course
                .map(c -> new CourseDTO()
                        .id(c.getId().toString())
                        .name(c.getName())
                        .code(c.getCode())).get());
    }

    @Override
    public ResponseEntity<List<CourseDTO>> getCourses(Boolean enabled) {
        List<Course> courses = courseService.getCourses(enabled);

        List<CourseDTO> coursesResponse = courses.stream().map(course ->
            new CourseDTO()
                .id(course.getId().toString())
                .term(course.getTerm())
                .enabled(course.isEnabled())
                .name(course.getName())
                .code(course.getCode())
        ).toList();

        return ResponseEntity.ok(coursesResponse);
    }

    @Override
    public ResponseEntity<CourseDTO> getCourse(String id, List<String> include) {
        Optional<Course> course = courseService.getCourse(UUID.fromString(id), include);

        if(course.isEmpty()) {
            // need to do this with error controller
            return ResponseEntity.badRequest().build();
        }

        CourseDTO courseDto = new CourseDTO();

        courseDto.name(course.get().getName())
                .id(course.get().getId().toString())
                .enabled(course.get().isEnabled())
                .term(course.get().getTerm())
                .code(course.get().getCode());

        if (include.contains("members")) {
            courseDto.setMembers(course.get().getMembers().stream().map(member ->
                    new CourseMemberDTO()
                            .canvasId(member.getCanvasId())
                            .courseRole(CourseMemberDTO.CourseRoleEnum.fromValue(member.getRole().name()))
                            .cwid(member.getUser().getCwid())
                            .sections(member.getSections().stream().map(Section::getName).toList())
            ).toList());
        }

        if (include.contains("assignments")) {
            courseDto.setAssignments(course.get().getAssignments().stream().map(assignment ->
                    new AssignmentDTO()
                            .category(assignment.getCategory())
                            .dueDate(assignment.getDueDate())
                            .unlockDate(assignment.getUnlockDate())
                            .enabled(assignment.isEnabled())
                            .points(assignment.getPoints())
            ).toList());
        }

        if (include.contains("sections")) {
            courseDto.setSections(course.get().getSections().stream().map(Section::getName).toList());
        }

        return ResponseEntity.ok(courseDto);
    }

    @Override
    public ResponseEntity<Void> enableCourse(String courseId) {
        courseService.enableCourse(UUID.fromString(courseId));
        return ResponseEntity.accepted().build();
    }

    @Override
    public ResponseEntity<Void> disableCourse(String courseId) {
        courseService.disableCourse(UUID.fromString(courseId));
        return ResponseEntity.accepted().build();
    }
}
