package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.events.NewTaskEvent;
import edu.mines.gradingadmin.managers.IdentityProvider;
import edu.mines.gradingadmin.managers.ImpersonationManager;
import edu.mines.gradingadmin.models.*;
import edu.mines.gradingadmin.models.tasks.ScheduledTaskDef;
import edu.mines.gradingadmin.models.tasks.UserImportTaskDef;
import edu.mines.gradingadmin.repositories.CourseMemberRepo;
import edu.mines.gradingadmin.repositories.ScheduledTaskRepo;
import edu.mines.gradingadmin.repositories.UserRepo;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CourseMemberService {
    private final CourseMemberRepo courseMemberRepo;
    private final ScheduledTaskRepo<UserImportTaskDef> taskRepo;

    private final UserService userService;
    private final SectionService sectionService;
    private final CourseService courseService;
    private final CanvasService canvasService;

    private final ApplicationEventPublisher eventPublisher;
    private final ImpersonationManager impersonationManager;

    public CourseMemberService(CourseMemberRepo courseMemberRepo, ScheduledTaskRepo<UserImportTaskDef> taskRepo, UserService userService, SectionService sectionService, CourseService courseService, CanvasService canvasService, ApplicationEventPublisher eventPublisher, ImpersonationManager impersonationManager) {
        this.courseMemberRepo = courseMemberRepo;
        this.taskRepo = taskRepo;
        this.userService = userService;
        this.sectionService = sectionService;
        this.courseService = courseService;
        this.canvasService = canvasService;
        this.eventPublisher = eventPublisher;
        this.impersonationManager = impersonationManager;
    }

    public List<CourseMember> searchCourseMembers(Course course, List<CourseRole> roles, String name, String cwid) {
        if(name != null) {
            return courseMemberRepo.findAllByCourseByUserName(course, name).stream().filter(x -> roles.contains(x.getRole())).toList();
        }
        else if(cwid != null) {
            return courseMemberRepo.findAllByCourseByCwid(course, cwid).stream().filter(x -> roles.contains(x.getRole())).toList();
        }
        return courseMemberRepo.getAllByCourse(course).stream().filter(x -> roles.contains(x.getRole())).toList();
    }

    // todo: break up this function a bit more
    public void syncCourseMembersTask(UserImportTaskDef task){
        Optional<Course> course = courseService.getCourse(task.getCourseToImport());
        if (course.isEmpty()){
            log.warn("Course '{}' does not exist!", task.getCourseToImport());
            return;
        }

        Map<String, Section> sections = sectionService.getSectionsForCourse(task.getCourseToImport()).stream().collect(Collectors.toUnmodifiableMap(s -> String.valueOf(s.getCanvasId()), s-> s));

        if (sections.isEmpty()){
            log.warn("Course '{}' has no sections!", course.get().getCode());
            return;
        }

        IdentityProvider impersonatedUser = impersonationManager.impersonateUser(task.getCreatedByUser());

        Map<String, edu.ksu.canvas.model.User> canvasUsersForCourse = canvasService.asUser(impersonatedUser).getCourseMembers(course.get().getCanvasId());

        List<User> users = userService.getOrCreateUsersFromCanvas(canvasUsersForCourse);

        Set<CourseMember> members = new HashSet<>();

        for(User user : users){
            log.trace("Processing user: {}", user);
            if (!canvasUsersForCourse.containsKey(user.getCwid())){
                log.warn("Requested user '{}' is not a member of requested class {}", user.getEmail(), course.get().getCode());
                continue;
            }

            edu.ksu.canvas.model.User canvasUser = canvasUsersForCourse.get(user.getCwid());

            if (canvasUser.getEnrollments().isEmpty()) {
                log.warn("User '{}' is not enrolled in any sections!", user.getEmail());
            }

            if (!canvasUser.getEnrollments().stream().allMatch(e -> sections.containsKey(e.getCourseSectionId()))) {
                // this shouldn't be possible
                log.warn("Requested sections for user '{}' do not exist!", user.getEmail());
                continue;
            }

            Set<Section> enrolledSections = canvasUser.getEnrollments().stream().map(e -> sections.get(e.getCourseSectionId())).collect(Collectors.toSet());

            Optional<CourseRole> role = canvasService.mapEnrollmentToRole(canvasUser.getEnrollments().getFirst());

            // acting user should be made the owner of the class
            if (user.equals(task.getCreatedByUser())) {
                role = Optional.of(CourseRole.OWNER);
            }

            if (role.isEmpty()) {
                log.warn("Missing role '{}' for user '{}'", canvasUser.getEnrollments().getFirst().getType(), user.getEmail());
                continue;
            }

            CourseMember newMembership = new CourseMember();
            newMembership.setCanvasId(String.valueOf(canvasUser.getId()));
            newMembership.setCourse(course.get());
            newMembership.setSections(enrolledSections);
            newMembership.setUser(user);
            newMembership.setRole(role.get());


            members.add(newMembership);
        }

        log.info("Saving {} course memberships for '{}'", members.size(), course.get().getCode());
        // this is quite large, so doing it at once will be a lot faster than saving incrementally
        courseMemberRepo.saveAll(members);
    }

    public Optional<ScheduledTaskDef> addMembersToCourse(User actingUser, Set<Long> dependencies, UUID courseId) {
        UserImportTaskDef task = new UserImportTaskDef();
        task.setCreatedByUser(actingUser);
        task.setCourseToImport(courseId);
        task = taskRepo.save(task);

        NewTaskEvent.TaskData<UserImportTaskDef> taskDefinition = new NewTaskEvent.TaskData<>(taskRepo, task.getId(), this::syncCourseMembersTask);
        taskDefinition.setDependsOn(dependencies);

        eventPublisher.publishEvent(new NewTaskEvent(this, taskDefinition));

        return Optional.of(task);
    }

    public Optional<CourseMember> addMemberToCourse(String courseId, String cwid, String canvasId, CourseRole role) {
        Optional<Course> course = courseService.getCourse(UUID.fromString(courseId));
        if(course.isEmpty()) {
            return Optional.empty();
        }

        Optional<User> user = userService.getUserByCwid(cwid);
        if(user.isEmpty()) {
            return Optional.empty();
        }

        CourseMember member = new CourseMember();
        member.setRole(role);
        member.setCanvasId(canvasId);
        member.setUser(user.get());
        member.setCourse(course.get());
        return Optional.of(courseMemberRepo.save(member));
    }

    public List<CourseRole> getRolesForUserAndCourse(User user, UUID courseId){
        Optional<Course> course = courseService.getCourse(courseId);

        if (course.isEmpty()){
            return List.of();
        }

        Set<CourseMember> memberships = courseMemberRepo.getByUserAndCourse(user, course.get());

        return memberships.stream().map(CourseMember::getRole).toList();
    }

    public Set<Section> getSectionsForUserAndCourse(User user, Course course){
        Set<CourseMember> memberships = courseMemberRepo.getByUserAndCourse(user, course);

        return memberships.stream().map(CourseMember::getSections).flatMap(Set::stream).collect(Collectors.toSet());
    }
}
