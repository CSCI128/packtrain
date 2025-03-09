package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.data.AssignmentDTO;
import edu.mines.gradingadmin.data.ExtensionDTO;
import edu.mines.gradingadmin.data.LateRequestDTO;
import edu.mines.gradingadmin.models.*;
import edu.mines.gradingadmin.repositories.AssignmentRepo;
import edu.mines.gradingadmin.repositories.ExtensionRepo;
import edu.mines.gradingadmin.repositories.LateRequestRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class ExtensionService {
    private final ExtensionRepo extensionRepo;
    private final LateRequestRepo lateRequestRepo;
    private final AssignmentRepo assignmentRepo;
    private final CourseMemberService courseMemberService;
    private final CourseService courseService;

    public ExtensionService(ExtensionRepo extensionRepo, LateRequestRepo lateRequestRepo, AssignmentRepo assignmentRepo, CourseMemberService courseMemberService, CourseService courseService) {
        this.extensionRepo = extensionRepo;
        this.lateRequestRepo = lateRequestRepo;
        this.assignmentRepo = assignmentRepo;
        this.courseMemberService = courseMemberService;
        this.courseService = courseService;
    }

    public List<Extension> getExtensionsByMigrationId(String migrationId) {
        return extensionRepo.getExtensionsByMigrationId(UUID.fromString(migrationId));
    }

    public List<LateRequest> getAllLateRequestsForStudent(String courseId, User user) {
        return extensionRepo.getAllLateRequestsForStudent(UUID.fromString(courseId), user);
    }

    public LateRequest createLateRequest(String courseId,
                                         User user,
                                         LateRequestDTO.RequestTypeEnum requestType,
                                         int daysRequested,
                                         Instant submissionDate,
                                         AssignmentDTO assignment,
                                         LateRequestDTO.StatusEnum status,
                                         ExtensionDTO extension) {
        LateRequest lateRequest = new LateRequest();
        lateRequest.setDaysRequested(daysRequested);
        lateRequest.setRequestType(RequestType.valueOf(requestType.name()));
        lateRequest.setStatus(RequestStatus.valueOf(status.name()));
        lateRequest.setSubmissionDate(submissionDate);
        if(extension != null) {
            Extension newExtension = new Extension();
            newExtension.setReason(extension.getReason());
            newExtension.setComments(extension.getComments());

            Optional<Course> course = courseService.getCourse(UUID.fromString(courseId));
            if(course.isPresent()) {
                Set<Section> sections = courseMemberService.getSectionsForUserAndCourse(user, course.get());
                Optional<Section> userSection = sections.stream().findFirst();
                if(userSection.isPresent()) {
                    Optional<CourseMember> instructor = sections.stream().findFirst().get().getMembers().stream().filter(x -> x.getRole() == CourseRole.INSTRUCTOR).findFirst();
                    if(instructor.isPresent()) {
                        newExtension.setReviewer(instructor.get().getUser());
                    }
                }
            }

            newExtension.setReviewerResponse(extension.getResponseToRequester());
            newExtension.setReviewerResponseTimestamp(extension.getResponseTimestamp());
            newExtension = extensionRepo.save(newExtension);
            lateRequest.setExtension(newExtension);
        }
        Optional<Assignment> foundAssignment = assignmentRepo.getAssignmentById(UUID.fromString(assignment.getId()));
        foundAssignment.ifPresent(lateRequest::setAssignment);
        lateRequest.setRequestingUser(user);
        return lateRequestRepo.save(lateRequest);
    }
}
