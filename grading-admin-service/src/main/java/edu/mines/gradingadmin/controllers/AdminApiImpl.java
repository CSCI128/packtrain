package edu.mines.gradingadmin.controllers;

import edu.mines.gradingadmin.api.AdminApiDelegate;
import edu.mines.gradingadmin.data.*;
import edu.mines.gradingadmin.factories.DTOFactory;
import edu.mines.gradingadmin.managers.SecurityManager;
import edu.mines.gradingadmin.models.*;
import edu.mines.gradingadmin.models.enums.CourseRole;
import edu.mines.gradingadmin.models.tasks.ScheduledTaskDef;
import edu.mines.gradingadmin.services.*;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Transactional
@Controller
public class AdminApiImpl implements AdminApiDelegate {

    private final CourseService courseService;
    private final SecurityManager securityManager;
    private final UserService userService;

    public AdminApiImpl(CourseService courseService, SecurityManager securityManager, UserService userService) {
        this.courseService = courseService;
        this.securityManager = securityManager;
        this.userService = userService;
    }


    @Override
    public ResponseEntity<CourseDTO> newCourse(CourseDTO courseDTO) {
        Optional<Course> course = courseService.createNewCourse(courseDTO);
        if (course.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(DTOFactory.toDto(course.get()));
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
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Override
    public ResponseEntity<UserDTO> adminUpdateUser(UserDTO userDTO) {
        Optional<User> user;

        if(userDTO.getAdmin()) {
            user = userService.makeAdmin(userDTO.getCwid());
        } else {
            user = userService.demoteAdmin(securityManager.getUser(), userDTO.getCwid());
        }

        if (user.isEmpty()){
            return ResponseEntity.badRequest().build();
        }

        user = userService.updateUser(userDTO);

        if(userDTO.getEnabled()) {
            user = userService.enableUser(userDTO.getCwid());
        }
        else {
            user = userService.disableUser(securityManager.getUser(), userDTO.getCwid());
        }

        return ResponseEntity.accepted().body(DTOFactory.toDto(user.get()));
    }

    @Override
    public ResponseEntity<Void> enableUser(String cwid) {
        Optional<User> user = userService.enableUser(cwid);

        if (user.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.accepted().build();

    }

    @Override
    public ResponseEntity<Void> disableUser(String cwid) {
        Optional<User> user = userService.disableUser(securityManager.getUser(), cwid);

        if (user.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.accepted().build();
    }
}
