package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.managers.SecurityManager;
import edu.mines.gradingadmin.models.*;
import edu.mines.gradingadmin.repositories.CourseMemberRepo;
import edu.mines.gradingadmin.repositories.CourseRepo;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class CourseService {
    private final SecurityManager securityManager;

    private final CourseRepo courseRepo;
    private final CourseMemberRepo courseMemberRepo;

    private final CanvasService canvasService;

    private final SectionService sectionService;
    private final UserService userService;

    public CourseService(SecurityManager securityManager, CourseRepo courseRepo, CanvasService canvasService, SectionService sectionService, UserService userService, CourseMemberRepo courseMemberRepo) {
        this.securityManager = securityManager;
        this.courseRepo = courseRepo;
        this.canvasService = canvasService;
        this.sectionService = sectionService;
        this.userService = userService;
        this.courseMemberRepo = courseMemberRepo;
    }

    public List<Course> getCourses(boolean enabled) {
        if(enabled) {
            return courseRepo.getAll(enabled);
        }
        return courseRepo.getAll();
    }

    public Optional<Course> importCourseFromCanvas(String canvasId){
        if (courseRepo.existsByCanvasId(canvasId)){
            log.warn("Course '{}' has already been created!", canvasId);
            return Optional.empty();
        }

        List<edu.ksu.canvas.model.Course> availableCourses =
                canvasService.getAllAvailableCourses()
                    .stream()
                    .filter(course -> course.getId().toString().equals(canvasId))
                    .toList();

        if (availableCourses.isEmpty()){
            return Optional.empty();
        }

        if (availableCourses.size() != 1){
            return Optional.empty();
        }

        edu.ksu.canvas.model.Course canvasCourse = availableCourses.getFirst();

        Course newCourse = new Course();
        newCourse.setCanvasId(canvasCourse.getId().toString());
        newCourse.setCode(canvasCourse.getCourseCode());
        newCourse.setName(canvasCourse.getName());
        newCourse.setTerm(canvasCourse.getTermId());
        newCourse.setEnabled(true);
        newCourse = courseRepo.save(newCourse);

        Map<String, Section> sections = sectionService.createSectionsFromCanvas(newCourse);
        newCourse.setSections(new HashSet<>(sections.values()));

        Map<String, edu.ksu.canvas.model.User> canvasUsers = canvasService.getCourseMembers(canvasId);
        Set<CourseMember> members = addMembersToCourse(newCourse, canvasUsers, sections);

        newCourse.setMembers(members);

        return Optional.of(newCourse);
    }


    public Set<CourseMember> addMembersToCourse(Course course, Map<String, edu.ksu.canvas.model.User> canvasUsers, Map<String, Section> sections){
        List<User> users = userService.createUsersFromCanvas(canvasUsers);

        Set<CourseMember> members = new HashSet<>();

        for (var user : users){
            if (!canvasUsers.containsKey(user.getCwid())){
                // this shouldn't be possible
                log.warn("Requested user '{}' is not a member of requested class {}", user.getEmail(), course.getCode());
                continue;
            }

            var canvasUser = canvasUsers.get(user.getCwid());

            if (canvasUser.getEnrollments().isEmpty()){
                log.warn("User '{}' is not enrolled in any sections!", user.getEmail());
            }

            if (!canvasUser.getEnrollments().stream().allMatch(e -> sections.containsKey(e.getCourseSectionId()))){
                // this shouldn't be possible
                log.warn("Requested sections for user '{}' do not exist!", user.getEmail());
                continue;
            }

            Set<Section> enrolledSections = canvasUser.getEnrollments().stream().map(e -> sections.get(e.getCourseSectionId())).collect(Collectors.toSet());

            Optional<CourseRole> role = canvasService.mapEnrollmentToRole(canvasUser.getEnrollments().getFirst());

            // acting user should be made the owner of the class
            if (user.equals(securityManager.getUser())){
                role = Optional.of(CourseRole.OWNER);
            }

            if (role.isEmpty()){
                log.warn("Missing role for user '{}'", user.getEmail());
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

        log.info("Saving {} course memberships for '{}'", members.size(), course.getCode());
        // this is quite large, so doing it at once will be a lot faster than saving incrementally
        courseMemberRepo.saveAll(members);

        members.clear();

        // have to look up the members again
        return courseMemberRepo.getAllByCourse(course);
    }

}
