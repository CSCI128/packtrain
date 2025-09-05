package edu.mines.packtrain.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.mines.packtrain.data.CourseMemberDTO;
import edu.mines.packtrain.data.websockets.CourseSyncNotificationDTO;
import edu.mines.packtrain.events.NewTaskEvent;
import edu.mines.packtrain.managers.IdentityProvider;
import edu.mines.packtrain.managers.ImpersonationManager;
import edu.mines.packtrain.models.Course;
import edu.mines.packtrain.models.CourseMember;
import edu.mines.packtrain.models.Section;
import edu.mines.packtrain.models.User;
import edu.mines.packtrain.models.enums.CourseRole;
import edu.mines.packtrain.models.tasks.ScheduledTaskDef;
import edu.mines.packtrain.models.tasks.UserSyncTaskDef;
import edu.mines.packtrain.repositories.CourseMemberRepo;
import edu.mines.packtrain.repositories.ScheduledTaskRepo;
import edu.mines.packtrain.services.external.CanvasService;
import jakarta.transaction.Transactional;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
public class CourseMemberService {
    private final CourseMemberRepo courseMemberRepo;
    private final ScheduledTaskRepo<UserSyncTaskDef> taskRepo;

    private final UserService userService;
    private final SectionService sectionService;
    private final CourseService courseService;
    private final CanvasService canvasService;

    private final ApplicationEventPublisher eventPublisher;
    private final ImpersonationManager impersonationManager;

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CourseMemberService(CourseMemberRepo courseMemberRepo, ScheduledTaskRepo<UserSyncTaskDef> taskRepo, UserService userService, SectionService sectionService, CourseService courseService, CanvasService canvasService, ApplicationEventPublisher eventPublisher, ImpersonationManager impersonationManager, SimpMessagingTemplate messagingTemplate) {
        this.courseMemberRepo = courseMemberRepo;
        this.taskRepo = taskRepo;
        this.userService = userService;
        this.sectionService = sectionService;
        this.courseService = courseService;
        this.canvasService = canvasService;
        this.eventPublisher = eventPublisher;
        this.impersonationManager = impersonationManager;
        this.messagingTemplate = messagingTemplate;
    }

    public Optional<CourseMember> findCourseMemberGivenCourseAndCwid(Course course, String cwid) {
        // TODO CODE SMELLS - need to add unique constant
        Optional<CourseMember> courseMember = courseMemberRepo.findAllByCourseByCwid(course, cwid)
                .stream().findFirst();
        if (courseMember.isEmpty()) {
            return Optional.empty();
        }

        courseMember.get().setSections(sectionService.getSectionByMember(courseMember.get()));
        return courseMember;
    }

    public CourseMember getStudentInstructor(Course course, Optional<Section> section) {
        // prefer who ever has less sections - coordinator is listed as instructor for all
        // adjunct classes. if no instructor exists for that section, then return the owner

        if (section.isEmpty()) {
            return getCourseOwner(course);
        }

        List<CourseMember> members = section.get().getMembers().stream()
                .filter(m -> m.getRole().equals(CourseRole.INSTRUCTOR)
                        || m.getRole().equals(CourseRole.OWNER))
                .sorted(Comparator.comparingInt(a -> a.getSections().size())).toList();

        if (members.isEmpty()) {
            return getCourseOwner(course);
        }


        return members.getFirst();
    }

    public Set<CourseMember> getAllMembersGivenCourse(Course course) {
        Set<CourseMember> members = courseMemberRepo.getAllByCourse(course);
        members.forEach(m -> m.setSections(sectionService.getSectionByMember(m)));

        return members;
    }

    @Transactional
    public List<CourseMember> searchCourseMembers(Course course, List<CourseRole> roles,
                                                  String name, String cwid) {
        if (name != null) {
            return courseMemberRepo.findAllByCourseByUserName(course, name).stream()
                    .filter(x -> roles.contains(x.getRole())).toList();
        } else if (cwid != null) {
            return courseMemberRepo.findAllByCourseByCwid(course, cwid).stream()
                    .filter(x -> roles.contains(x.getRole())).toList();
        }
        return courseMemberRepo.getAllByCourse(course).stream()
                .filter(x -> roles.contains(x.getRole())).toList();
    }

    public CourseMember getCourseOwner(Course course) {
        Optional<CourseMember> owner = courseMemberRepo.getAllByCourse(course).stream()
                .filter(m -> m.getRole().equals(CourseRole.OWNER)).findFirst();

        if (owner.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    String.format("No owner configured for course '%s'", course.getCode()));
        }

        return owner.get();

    }

    public List<CourseMember> getAllStudentsInCourse(Course course) {
        return courseMemberRepo.getAllByCourse(course).stream()
                .filter(x -> x.getRole().equals(CourseRole.STUDENT)).toList();
    }

    @Transactional
    public void syncCourseMembersTask(UserSyncTaskDef task) {
        if (!task.shouldAddNewUsers() && !task.shouldUpdateExistingUsers()
                && !task.shouldRemoveOldUsers()) {
            log.warn("No user sync action should be taken. Skipping task");
            return;
        }

        Course course = courseService.getCourse(task.getCourseToImport());

        Map<String, Section> sections = sectionService.getSectionsForCourse(task
                .getCourseToImport()).stream()
                .collect(Collectors.toUnmodifiableMap(
                        s -> String.valueOf(s.getCanvasId()), s -> s));

        if (sections.isEmpty()) {
            log.warn("Course '{}' has no sections!", course.getCode());
            return;
        }

        IdentityProvider impersonatedUser = impersonationManager.impersonateUser(
                task.getCreatedByUser());

        Map<String, edu.ksu.canvas.model.User> canvasUsersForCourse = canvasService.asUser(
                impersonatedUser).getCourseMembers(course.getCanvasId());

        List<User> users = userService.getOrCreateUsersFromCanvas(canvasUsersForCourse);

        Set<String> incomingCwids = users.stream().map(User::getCwid).collect(Collectors.toSet());

        Set<String> existingCwids = courseMemberRepo.getAllCwidsByCourse(course);

        // why the heck does java not define a proper difference function?
        Set<String> cwidsToCreate = incomingCwids.stream()
                .filter(c -> !existingCwids.contains(c)).collect(Collectors.toSet());

        Set<String> cwidsToRemove = existingCwids.stream()
                .filter(c -> !incomingCwids.contains(c)).collect(Collectors.toSet());

        Set<String> cwidsToUpdate = incomingCwids.stream()
                .filter(c -> !cwidsToCreate.contains(c)).collect(Collectors.toSet());

        if (task.shouldAddNewUsers()) {
            Set<CourseMember> newMembers = createNewEnrollments(
                    task,
                    users.stream().filter(u -> cwidsToCreate.contains(u.getCwid())).toList(),
                    canvasUsersForCourse,
                    course, sections
            );

            log.info("Saving {} new course memberships for '{}'",
                    newMembers.size(), course.getCode());
            // this is quite large, so doing it at once will be a lot
            // faster than saving incrementally
            courseMemberRepo.saveAll(newMembers);
        }


        if (task.shouldRemoveOldUsers()) {
            log.info("Deleting {} course memberships for '{}'", cwidsToRemove.size(), course.getCode());
            if (!cwidsToRemove.isEmpty()) {
                courseMemberRepo.deleteByCourseAndCwid(course, cwidsToRemove);
            }
        }

        if (task.shouldUpdateExistingUsers()) {
            Set<CourseMember> updatedMembers = updateExistingEnrollments(task, cwidsToUpdate, canvasUsersForCourse, course, sections);

            log.info("Updating {} course memberships for '{}'", updatedMembers.size(), course.getCode());

            courseMemberRepo.saveAll(updatedMembers);
        }
    }

    @Transactional
    protected Set<CourseMember> updateExistingEnrollments(UserSyncTaskDef task, Set<String> users, Map<String, edu.ksu.canvas.model.User> canvasUsersForCourse, Course course, Map<String, Section> sections) {
        Map<String, CourseMember> members = courseMemberRepo.getAllByCourseAndCwids(course, users).stream().collect(Collectors.toMap(c -> c.getUser().getCwid(), c -> c));

        for (String user : members.keySet()) {
            log.trace("Updating membership for user: {}", user);

            CourseMember member = members.get(user);

            edu.ksu.canvas.model.User canvasUser = canvasUsersForCourse.get(user);
            // make sure that we keep canvasIds up to date (mostly because they can be self when a course is created)
            member.setCanvasId(String.valueOf(canvasUser.getId()));

            if (canvasUser.getEnrollments().isEmpty()) {
                log.warn("User '{}' is not enrolled in any sections!", user);
            }
            if (!canvasUser.getEnrollments().stream().allMatch(e -> sections.containsKey(e.getCourseSectionId()))) {
                // this shouldn't be possible
                log.warn("Requested sections for user '{}' do not exist!", user);
                continue;
            }

            Set<Section> enrolledSections = canvasUser.getEnrollments().stream().map(e -> sections.get(e.getCourseSectionId())).collect(Collectors.toSet());

            Optional<CourseRole> role = canvasService.mapEnrollmentToRole(canvasUser.getEnrollments().getFirst());

            if (role.isEmpty()) {
                log.warn("Missing role '{}' for user '{}'", canvasUser.getEnrollments().getFirst().getType(), user);
                continue;
            }

            member.setSections(enrolledSections);
            if (member.getRole() != CourseRole.OWNER) {
                member.setRole(role.get());
            }

            members.put(user, member);
        }

        return new HashSet<>(members.values());

    }

    private Set<CourseMember> createNewEnrollments(UserSyncTaskDef task, List<User> users, Map<String, edu.ksu.canvas.model.User> canvasUsersForCourse, Course course, Map<String, Section> sections) {
        Set<CourseMember> members = new HashSet<>();

        for (User user : users) {
            log.trace("Processing user: {}", user);

            // we dont need to worry about this case, if we get here then it is in the course.
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
            newMembership.setCourse(course);
            newMembership.setSections(enrolledSections);
            newMembership.setUser(user);
            newMembership.setRole(role.get());


            members.add(newMembership);
        }
        return members;
    }

    public ScheduledTaskDef syncMembersFromCanvas(User actingUser, Set<Long> dependencies, UUID courseId, boolean updateExisting, boolean removeOld, boolean addNew) {
        UserSyncTaskDef task = new UserSyncTaskDef();
        task.setCreatedByUser(actingUser);
        task.setTaskName(String.format("Sync Course '%s': Course Members", courseId));
        task.setCourseToImport(courseId);
        task.shouldUpdateExistingUsers(updateExisting);
        task.shouldAddNewUsers(addNew);
        task.shouldRemoveOldUsers(removeOld);
        task = taskRepo.save(task);

        CourseSyncNotificationDTO notificationDTO = CourseSyncNotificationDTO.builder().membersComplete(true).build();
        NewTaskEvent.TaskData<UserSyncTaskDef> taskDefinition = new NewTaskEvent.TaskData<>(taskRepo, task.getId(), this::syncCourseMembersTask);
        taskDefinition.setDependsOn(dependencies);
        taskDefinition.setOnJobComplete(Optional.of(_ -> {
            try {
                messagingTemplate.convertAndSend("/courses/import", objectMapper.writeValueAsString(notificationDTO));
            } catch (JsonProcessingException _) {
                throw new RuntimeException("Could not process JSON for sending notification DTO!");
            }
        }));

        eventPublisher.publishEvent(new NewTaskEvent(this, taskDefinition));

        return task;
    }

    public CourseMember addMemberToCourse(String courseId, CourseMemberDTO courseMemberDTO) {
        Course course = courseService.getCourse(UUID.fromString(courseId));

        User user = userService.getUserByCwid(courseMemberDTO.getCwid());

        CourseMember member = new CourseMember();
        member.setRole(CourseRole.fromString(courseMemberDTO.getCourseRole().getValue()));
        member.setCanvasId(courseMemberDTO.getCanvasId());
        member.setUser(user);
        member.setCourse(course);

        return courseMemberRepo.save(member);
    }

    public boolean removeMembershipForUserAndCourse(User user, String courseId) {
        Course course = courseService.getCourse(UUID.fromString(courseId));

        Optional<CourseMember> courseMember = courseMemberRepo.getAllByCourseAndUser(course, user);

        if (courseMember.isEmpty()) {
            log.warn("User '{}' is not enrolled in course '{}'!", user.getEmail(), course.getCode());
            return false;
        }

        if (courseMember.get().getRole() == CourseRole.OWNER) {
            log.warn("Refusing to remove owner '{}' from '{}'!", user.getEmail(), course.getCode());
            return false;
        }

        courseMemberRepo.delete(courseMember.get());

        log.info("Removed user '{}' from course '{}'", user.getEmail(), course.getCode());
        return true;
    }

    public CourseRole getRoleForUserAndCourse(User user, UUID courseId) {
        Course course = courseService.getCourse(courseId);

        Optional<CourseMember> membership = courseMemberRepo.getByUserAndCourse(user, course);

        if (membership.isEmpty()) {
            return CourseRole.NOT_ENROLLED;
        }

        return membership.get().getRole();
    }

    public Set<Section> getSectionsForUserAndCourse(User user, Course course) {
        Optional<CourseMember> membership = courseMemberRepo.getByUserAndCourse(user, course);

        if (membership.isEmpty()) {
            return Set.of();
        }

        return membership.get().getSections();
    }

    public void useLatePasses(Course course, User user, double amount) {
        Optional<CourseMember> courseMember = courseMemberRepo.findAllByCourseByCwid(course, user.getCwid()).stream().findFirst();

        if (courseMember.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("User '%s' is not enrolled in course '%s'", user.getEmail(), course.getCode()));
        }

        double finalLatePasses = courseMember.get().getLatePassesUsed() + amount;
        courseMember.get().setLatePassesUsed(finalLatePasses);
        courseMemberRepo.save(courseMember.get());
    }

    public void refundLatePasses(Course course, User user, double amount) {
        Optional<CourseMember> courseMember = courseMemberRepo.findAllByCourseByCwid(course, user.getCwid()).stream().findFirst();

        if (courseMember.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("User '%s' is not enrolled in course '%s'", user.getEmail(), course.getCode()));
        }

        double finalLatePasses = courseMember.get().getLatePassesUsed() - amount;
        courseMember.get().setLatePassesUsed(finalLatePasses);
        courseMemberRepo.save(courseMember.get());
    }

    public Optional<String> getCwidGivenCourseAndEmail(String email, Course course) {
        User user = userService.getUserByEmail(email);

        if (!courseMemberRepo.existsByCourseAndUser(course, user)) {
            log.warn("User '{}' is not enrolled in course '{}'", user.getEmail(), course.getCode());
            return Optional.empty();
        }

        return Optional.of(user.getCwid());
    }

    public String getCanvasIdGivenCourseAndCwid(String cwid, Course course) {
        User user = userService.getUserByCwid(cwid);

        Optional<CourseMember> member = courseMemberRepo.getByUserAndCourse(user, course);

        if (member.isEmpty()) {
            log.warn("User '{}' is not enrolled in course '{}'", user.getCwid(), course.getCode());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not enrolled in this course");
        }

        return member.get().getCanvasId();
    }

    public boolean isUserEnrolledInCourse(String cwid, Course course) {
        return courseMemberRepo.existsByCourseAndId(course, cwid);
    }
}
