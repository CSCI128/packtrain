package edu.mines.packtrain.controllers;

import edu.mines.packtrain.api.AdminApiDelegate;
import edu.mines.packtrain.data.CourseDTO;
import edu.mines.packtrain.data.CourseMemberDTO;
import edu.mines.packtrain.data.UserDTO;
import edu.mines.packtrain.factories.DTOFactory;
import edu.mines.packtrain.managers.SecurityManager;
import edu.mines.packtrain.models.Course;
import edu.mines.packtrain.models.User;
import edu.mines.packtrain.services.CourseMemberService;
import edu.mines.packtrain.services.CourseService;
import edu.mines.packtrain.services.UserService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Transactional
@Controller
public class AdminApiImpl implements AdminApiDelegate {

    private final CourseService courseService;
    private final SecurityManager securityManager;
    private final UserService userService;
    private final CourseMemberService courseMemberService;

    public AdminApiImpl(CourseService courseService, SecurityManager securityManager, UserService userService, CourseMemberService courseMemberService) {
        this.courseService = courseService;
        this.securityManager = securityManager;
        this.userService = userService;
        this.courseMemberService = courseMemberService;
    }


    @Override
    public ResponseEntity<CourseDTO> newCourse(CourseDTO courseDTO) {
        Course course = courseService.createNewCourse(courseDTO);

        courseMemberService.addMemberToCourse(course.getId().toString(), new CourseMemberDTO()
                .canvasId("self")
                .cwid(securityManager.getCwid())
                .courseRole(CourseMemberDTO.CourseRoleEnum.OWNER)
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(DTOFactory.toDto(course));
    }

    @Override
    public ResponseEntity<Void> deleteCourse(UUID courseId) {
        if (!courseService.deleteCourse(courseId)) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Override
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users.stream().map(DTOFactory::toDto).toList());
    }


    @Override
    public ResponseEntity<Void> createUser(UserDTO userDTO) {
        Optional<User> user = userService.createNewUser(
                userDTO.getCwid(),
                userDTO.getAdmin(),
                userDTO.getName(),
                userDTO.getEmail()
        );

        if (user.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("User '%s' already exists!", userDTO.getCwid()));
        }

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Override
    public ResponseEntity<UserDTO> adminUpdateUser(UserDTO userDTO) {
        boolean adminRes = userDTO.getAdmin() ? userService.makeAdmin(securityManager.getUser(), userDTO.getCwid()) : userService.demoteAdmin(securityManager.getUser(), userDTO.getCwid());

        if (!adminRes){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to update admin for user!");
        }

        boolean disabledRes = userDTO.getEnabled() ? userService.enableUser(securityManager.getUser(), userDTO.getCwid()) : userService.disableUser(securityManager.getUser(), userDTO.getCwid());

        if (!disabledRes){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to update enabled for user!");
        }

        User user = userService.updateUser(userDTO);

        return ResponseEntity.accepted().body(DTOFactory.toDto(user));
    }

    @Override
    public ResponseEntity<Void> enableUser(String cwid) {
        boolean res = userService.enableUser(securityManager.getUser(), cwid);

        if (!res){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to update enabled for user!");
        }

        return ResponseEntity.noContent().build();

    }

    @Override
    public ResponseEntity<Void> disableUser(String cwid) {
        boolean res = userService.disableUser(securityManager.getUser(), cwid);

        if (!res){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to update enabled for user!");
        }

        return ResponseEntity.noContent().build();
    }
}
