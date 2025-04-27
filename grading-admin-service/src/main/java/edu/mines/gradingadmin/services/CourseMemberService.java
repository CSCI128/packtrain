package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.data.CourseMemberDTO;
import edu.mines.gradingadmin.events.NewTaskEvent;
import edu.mines.gradingadmin.managers.IdentityProvider;
import edu.mines.gradingadmin.managers.ImpersonationManager;
import edu.mines.gradingadmin.models.*;
import edu.mines.gradingadmin.models.enums.CourseRole;
import edu.mines.gradingadmin.models.tasks.ScheduledTaskDef;
import edu.mines.gradingadmin.models.tasks.UserSyncTaskDef;
import edu.mines.gradingadmin.repositories.CourseMemberRepo;
import edu.mines.gradingadmin.repositories.ScheduledTaskRepo;
import edu.mines.gradingadmin.services.external.CanvasService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

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

    public CourseMemberService(CourseMemberRepo courseMemberRepo, ScheduledTaskRepo<UserSyncTaskDef> taskRepo, UserService userService, SectionService sectionService, CourseService courseService, CanvasService canvasService, ApplicationEventPublisher eventPublisher, ImpersonationManager impersonationManager) {
        this.courseMemberRepo = courseMemberRepo;
        this.taskRepo = taskRepo;
        this.userService = userService;
        this.sectionService = sectionService;
        this.courseService = courseService;
        this.canvasService = canvasService;
        this.eventPublisher = eventPublisher;
        this.impersonationManager = impersonationManager;
    }

    public Optional<CourseMember> getCourseMemberByCourseByCwid(Course course, String cwid) {
        return courseMemberRepo.findAllByCourseByCwid(course, cwid).stream().findFirst();
    }

    public Optional<CourseMember> getFirstSectionInstructor(Section section) {
        return section.getMembers().stream().filter(member -> member.getRole() == CourseRole.INSTRUCTOR).findFirst();
    }

    public List<CourseMember> searchCourseMembers(Course course, List<CourseRole> roles, String name, String cwid) {
        if (name != null) {
            return courseMemberRepo.findAllByCourseByUserName(course, name).stream().filter(x -> roles.contains(x.getRole())).toList();
        } else if (cwid != null) {
            return courseMemberRepo.findAllByCourseByCwid(course, cwid).stream().filter(x -> roles.contains(x.getRole())).toList();
        }
        return courseMemberRepo.getAllByCourse(course).stream().filter(x -> roles.contains(x.getRole())).toList();
    }

    public List<CourseMember> getAllStudentsInCourse(Course course){
        return courseMemberRepo.getAllByCourse(course).stream().filter(x -> x.getRole().equals(CourseRole.STUDENT)).toList();
    }

    @Transactional
    public void syncCourseMembersTask(UserSyncTaskDef task) {
        if (!task.shouldAddNewUsers() && !task.shouldUpdateExistingUsers() && !task.shouldRemoveOldUsers()) {
            log.warn("No user sync action should be taken. Skipping task");
            return;
        }

        Optional<Course> course = courseService.getCourse(task.getCourseToImport());
        if (course.isEmpty()) {
            log.warn("Course '{}' does not exist!", task.getCourseToImport());
            return;
        }

        Map<String, Section> sections = sectionService.getSectionsForCourse(task.getCourseToImport()).stream().collect(Collectors.toUnmodifiableMap(s -> String.valueOf(s.getCanvasId()), s -> s));

        if (sections.isEmpty()) {
            log.warn("Course '{}' has no sections!", course.get().getCode());
            return;
        }

        IdentityProvider impersonatedUser = impersonationManager.impersonateUser(task.getCreatedByUser());

        Map<String, edu.ksu.canvas.model.User> canvasUsersForCourse = canvasService.asUser(impersonatedUser).getCourseMembers(course.get().getCanvasId());

        List<User> users = userService.getOrCreateUsersFromCanvas(canvasUsersForCourse);

        Set<String> incomingCwids = users.stream().map(User::getCwid).collect(Collectors.toSet());

        Set<String> existingCwids = courseMemberRepo.getAllCwidsByCourse(course.get());

        // why the heck does java not define a proper difference function?
        Set<String> cwidsToCreate = incomingCwids.stream().filter(c -> !existingCwids.contains(c)).collect(Collectors.toSet());

        Set<String> cwidsToRemove = existingCwids.stream().filter(c -> !incomingCwids.contains(c)).collect(Collectors.toSet());

        Set<String> cwidsToUpdate = incomingCwids.stream().filter(c -> !cwidsToCreate.contains(c)).collect(Collectors.toSet());

        if (task.shouldAddNewUsers()) {
            Set<CourseMember> newMembers = createNewEnrollments(
                    task,
                    users.stream().filter(u -> cwidsToCreate.contains(u.getCwid())).toList(),
                    canvasUsersForCourse,
                    course.get(), sections
            );

            log.info("Saving {} new course memberships for '{}'", newMembers.size(), course.get().getCode());
            // this is quite large, so doing it at once will be a lot faster than saving incrementally
            courseMemberRepo.saveAll(newMembers);
        }


        if (task.shouldRemoveOldUsers()) {
            log.info("Deleting {} course memberships for '{}'", cwidsToRemove.size(), course.get().getCode());
            if (!cwidsToRemove.isEmpty()) {
                courseMemberRepo.deleteByCourseAndCwid(course.get(), cwidsToRemove);
            }
        }

        if (task.shouldUpdateExistingUsers()) {
            Set<CourseMember> updatedMembers = updateExistingEnrollments(task, cwidsToUpdate, canvasUsersForCourse, course.get(), sections);

            log.info("Updating {} course memberships for '{}'", updatedMembers.size(), course.get().getCode());

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

    public Optional<ScheduledTaskDef> syncMembersFromCanvas(User actingUser, Set<Long> dependencies, UUID courseId, boolean updateExisting, boolean removeOld, boolean addNew) {
        UserSyncTaskDef task = new UserSyncTaskDef();
        task.setCreatedByUser(actingUser);
        task.setTaskName(String.format("Sync Course '%s': Course Members", courseId));
        task.setCourseToImport(courseId);
        task.shouldUpdateExistingUsers(updateExisting);
        task.shouldAddNewUsers(addNew);
        task.shouldRemoveOldUsers(removeOld);
        task = taskRepo.save(task);

        NewTaskEvent.TaskData<UserSyncTaskDef> taskDefinition = new NewTaskEvent.TaskData<>(taskRepo, task.getId(), this::syncCourseMembersTask);
        taskDefinition.setDependsOn(dependencies);

        eventPublisher.publishEvent(new NewTaskEvent(this, taskDefinition));

        return Optional.of(task);
    }

    public Optional<CourseMember> addMemberToCourse(String courseId, CourseMemberDTO courseMemberDTO) {
        Optional<Course> course = courseService.getCourse(UUID.fromString(courseId));
        if (course.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course does not exist");
        }

        Optional<User> user = userService.getUserByCwid(courseMemberDTO.getCwid());
        if (user.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User does not exist");
        }

        CourseMember member = new CourseMember();
        member.setRole(CourseRole.fromString(courseMemberDTO.getCourseRole().getValue()));
        member.setCanvasId(courseMemberDTO.getCanvasId());
        member.setUser(user.get());
        member.setCourse(course.get());
        return Optional.of(courseMemberRepo.save(member));
    }

    public boolean removeMembershipForUserAndCourse(User user, String courseId) {
        Optional<Course> course = courseService.getCourse(UUID.fromString(courseId));
        if (course.isEmpty()) {
            log.warn("Course '{}' does not exist!", courseId);
            return false;
        }

        Optional<CourseMember> courseMember = courseMemberRepo.getAllByCourseAndUser(course.get(), user);

        if (courseMember.isEmpty()) {
            log.warn("User '{}' is not enrolled in course '{}'!", user.getEmail(), course.get().getCode());
            return false;
        }

        if (courseMember.get().getRole() == CourseRole.OWNER) {
            log.warn("Attempt to remove owner '{}' from '{}'!", user.getEmail(), course.get().getCode());
            return false;
        }

        courseMemberRepo.delete(courseMember.get());
        // may want to do check to make sure that the user is not a admin
        log.info("Removed user '{}' from course '{}'", user.getEmail(), course.get().getCode());
        return true;
    }

    public CourseRole getRoleForUserAndCourse(User user, UUID courseId) {
        Optional<Course> course = courseService.getCourse(courseId);

        Optional<CourseMember> membership = courseMemberRepo.getByUserAndCourse(user, course.get());

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
        if(courseMember.isEmpty()) {
            return;
        }

        double finalLatePasses = courseMember.get().getLatePassesUsed() + amount;
        courseMember.get().setLatePassesUsed(finalLatePasses);
        courseMemberRepo.save(courseMember.get());
    }

    public void refundLatePasses(Course course, User user, double amount) {
        Optional<CourseMember> courseMember = courseMemberRepo.findAllByCourseByCwid(course, user.getCwid()).stream().findFirst();
        if (courseMember.isEmpty()) {
            return;
        }

        double finalLatePasses = courseMember.get().getLatePassesUsed() - amount;
        courseMember.get().setLatePassesUsed(finalLatePasses);
        courseMemberRepo.save(courseMember.get());
    }

    public Optional<String> getCwidGivenCourseAndEmail(String email, Course course){
        Optional<User> user = userService.getUserByEmail(email);

        if (user.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User does not exist");
        }

        if(!courseMemberRepo.existsByCourseAndUser(course, user.get())){
            log.warn("User '{}' is not enrolled in course '{}'", user.get().getCwid(), course.getCode());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not enrolled in this course");
        }

        return Optional.of(user.get().getCwid());
    }

    public String getCanvasIdGivenCourseAndCwid(String cwid, Course course){
        Optional<User> user = userService.getUserByCwid(cwid);

        if (user.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User does not exist");
        }

        Optional<CourseMember> member = courseMemberRepo.getByUserAndCourse(user.get(), course);

        if(member.isEmpty()){
            log.warn("User '{}' is not enrolled in course '{}'", user.get().getCwid(), course.getCode());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not enrolled in this course");
        }

        return member.get().getCanvasId();
    }
}
