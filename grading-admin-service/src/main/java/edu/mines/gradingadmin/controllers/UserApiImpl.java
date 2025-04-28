package edu.mines.gradingadmin.controllers;

import edu.mines.gradingadmin.api.UserApiDelegate;
import edu.mines.gradingadmin.data.CredentialDTO;
import edu.mines.gradingadmin.data.EnrollmentDTO;
import edu.mines.gradingadmin.data.UserDTO;
import edu.mines.gradingadmin.factories.DTOFactory;
import edu.mines.gradingadmin.managers.SecurityManager;
import edu.mines.gradingadmin.models.Course;
import edu.mines.gradingadmin.models.Credential;
import edu.mines.gradingadmin.models.User;
import edu.mines.gradingadmin.services.CourseMemberService;
import edu.mines.gradingadmin.services.CredentialService;
import edu.mines.gradingadmin.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
public class UserApiImpl implements UserApiDelegate {
    private final UserService userService;
    private final CredentialService credentialService;
    private final SecurityManager securityManager;
    private final CourseMemberService courseMemberService;

    public UserApiImpl(UserService userService, CredentialService credentialService, SecurityManager securityManager, CourseMemberService courseMemberService) {
        this.userService = userService;
        this.credentialService = credentialService;
        this.securityManager = securityManager;
        this.courseMemberService = courseMemberService;
    }

    @Override
    public ResponseEntity<UserDTO> getUser() {
        User user = securityManager.getUser();
        return ResponseEntity.ok(DTOFactory.toDto(user));
    }

    @Override
    public ResponseEntity<UserDTO> updateUser(UserDTO userDTO) {
        User user = userService.updateUser(userDTO);

        return ResponseEntity.accepted().body(DTOFactory.toDto(user));
    }

    @Override
    public ResponseEntity<CredentialDTO> newCredential(CredentialDTO credential) {
        Credential newCredential = credentialService.createNewCredentialForService(securityManager.getCwid(), credential);

        return ResponseEntity.status(HttpStatus.CREATED).body(DTOFactory.toDto(newCredential));
    }

    @Override
    public ResponseEntity<List<CredentialDTO>> getCredentials() {
        return ResponseEntity.ok(credentialService.getAllCredentials(securityManager.getCwid())
                .stream().map(DTOFactory::toDto).toList());
    }

    @Override
    public ResponseEntity<Void> markCredentialAsPrivate(String credentialId){
        credentialService.markCredentialAsPrivate(securityManager.getCwid(), UUID.fromString(credentialId));

        return ResponseEntity.accepted().build();
    }

    @Override
    public ResponseEntity<Void> markCredentialAsPublic(String credentialId){
        credentialService.markCredentialAsPublic(securityManager.getCwid(), UUID.fromString(credentialId));

        return ResponseEntity.accepted().build();
    }

    @Override
    public ResponseEntity<Void> deleteCredential(String credentialId){
        credentialService.deleteCredential(securityManager.getCwid(), UUID.fromString(credentialId));

        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<EnrollmentDTO>> getEnrollments() {
        User user = securityManager.getUser();

        List<Course> enrollments = userService.getEnrollments(user.getCwid());

        return ResponseEntity.ok(enrollments.stream().map(course -> new EnrollmentDTO()
                .id(course.getId().toString())
                .term(course.getTerm())
                .name(course.getName())
                .code(course.getCode())
                .cwid(user.getCwid())
                .courseRole(
                        EnrollmentDTO.CourseRoleEnum.fromValue(
                                courseMemberService.findCourseMemberGivenCourseAndCwid(course, user.getCwid())
                                        .map(member -> member.getRole().name().toLowerCase())
                                        .orElse("unknown")
                        ))
        ).toList());
    }
}
