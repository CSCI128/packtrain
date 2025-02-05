package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.models.*;
import edu.mines.gradingadmin.repositories.CourseMemberRepo;
import edu.mines.gradingadmin.repositories.CourseRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CourseService {

    private final CourseRepo courseRepo;
    private final CourseMemberRepo courseMemberRepo;

    private final CanvasService canvasService;

    private final SectionService sectionService;
    private final UserService userService;

    public CourseService(CourseRepo courseRepo, CanvasService canvasService, SectionService sectionService, UserService userService, CourseMemberRepo courseMemberRepo) {
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

    public void enableCourse(UUID courseId) {
        Optional<Course> course = courseRepo.findById(courseId);
        if(course.isPresent()) {
            course.get().setEnabled(true);
            courseRepo.save(course.get());
        }
    }

    public void disableCourse(UUID courseId) {
        Optional<Course> course = courseRepo.findById(courseId);
        if(course.isPresent()) {
            course.get().setEnabled(false);
            courseRepo.save(course.get());
        }
    }

    public Optional<UUID> createNewCourse(String name, String term, String courseCode){
        Course newCourse = new Course();
        newCourse.setName(name);
        newCourse.setCode(courseCode);
        newCourse.setTerm(term);
        newCourse.setEnabled(true);
        newCourse = courseRepo.save(newCourse);
        return Optional.of(newCourse.getId());
    }


    public Optional<Course> createNewCourse(String canvasId){
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
        // this is going to be super slow, I'm not really sure how to do this in better than o(n^2)

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

            if (role.isEmpty()){
                log.warn("Missing role for user '{}'", user.getEmail());
                continue;
            }


            CourseMember newMembership = new CourseMember();
            newMembership.setCanvasId(String.valueOf(canvasUser.getId()));
            newMembership.setCourse(course);
            newMembership.setSections(enrolledSections);
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
