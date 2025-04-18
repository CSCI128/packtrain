package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.data.ExtensionDTO;
import edu.mines.gradingadmin.data.LateRequestDTO;
import edu.mines.gradingadmin.models.*;
import edu.mines.gradingadmin.models.enums.LateRequestStatus;
import edu.mines.gradingadmin.models.enums.LateRequestType;
import edu.mines.gradingadmin.repositories.CourseMemberRepo;
import edu.mines.gradingadmin.repositories.ExtensionRepo;
import edu.mines.gradingadmin.repositories.LateRequestRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ExtensionService {
    private final ExtensionRepo extensionRepo;
    private final LateRequestRepo lateRequestRepo;
    private final AssignmentService assignmentService;
    private final CourseMemberService courseMemberService;
    private final CourseService courseService;

    public ExtensionService(ExtensionRepo extensionRepo, LateRequestRepo lateRequestRepo, AssignmentService assignmentService, CourseMemberService courseMemberService, CourseService courseService) {
        this.extensionRepo = extensionRepo;
        this.lateRequestRepo = lateRequestRepo;
        this.assignmentService = assignmentService;
        this.courseMemberService = courseMemberService;
        this.courseService = courseService;
    }

    public List<Extension> getExtensionsByMigrationId(String migrationId) {
        return extensionRepo.getExtensionsByMigrationId(UUID.fromString(migrationId));
    }

    public List<LateRequest> getAllLateRequests(String courseId, String lateRequestStatus) {
        if(lateRequestStatus == null) {
            return lateRequestRepo.getAllLateRequests(UUID.fromString(courseId));
        }
        else if(lateRequestStatus.equalsIgnoreCase("approved")) {
            return lateRequestRepo.getAllLateRequests(UUID.fromString(courseId)).stream().filter(c -> c.getStatus() == LateRequestStatus.APPROVED).toList();
        }
        else if(lateRequestStatus.equalsIgnoreCase("denied")) {
            return lateRequestRepo.getAllLateRequests(UUID.fromString(courseId)).stream().filter(c -> c.getStatus() == LateRequestStatus.REJECTED || c.getStatus() == LateRequestStatus.IGNORED).toList();
        }
        else if(lateRequestStatus.equalsIgnoreCase("pending")) {
            return lateRequestRepo.getAllLateRequests(UUID.fromString(courseId)).stream().filter(c -> c.getStatus() == LateRequestStatus.PENDING).toList();
        }
        return List.of();
    }

    public Optional<LateRequest> approveExtension(String assignmentId, String userId, String extensionId) {
        Optional<LateRequest> lateRequest = getLateRequest(UUID.fromString(extensionId));
        if(lateRequest.isPresent()) {
            LateRequest request = lateRequest.get();

            request.setStatus(LateRequestStatus.APPROVED);

            return Optional.of(lateRequestRepo.save(request));
        }
        return Optional.empty();
    }

    public Optional<LateRequest> denyExtension(String assignmentId, String userId, String extensionId) {
        Optional<LateRequest> lateRequest = getLateRequest(UUID.fromString(extensionId));
        if(lateRequest.isPresent()) {
            LateRequest request = lateRequest.get();

            request.setStatus(LateRequestStatus.REJECTED);

            return Optional.of(lateRequestRepo.save(request));
        }
        return Optional.empty();
    }

    public List<LateRequest> getAllLateRequestsForStudent(String courseId, User user) {
        return extensionRepo.getAllLateRequestsForStudent(UUID.fromString(courseId), user);
    }

    public Extension createExtensionFromDTO(String courseId, User user, ExtensionDTO extensionDTO) {
        Extension newExtension = new Extension();
        newExtension.setReason(extensionDTO.getReason());
        newExtension.setComments(extensionDTO.getComments());

        Optional<Course> course = courseService.getCourse(UUID.fromString(courseId));
        if(course.isPresent()) {
            Optional<Section> userSection = courseMemberService.getSectionsForUserAndCourse(user, course.get()).stream().findFirst();
            if(userSection.isPresent()) {
                Optional<CourseMember> instructor = courseMemberService.getFirstSectionInstructor(userSection.get());
                if(instructor.isPresent()) {
                    newExtension.setReviewer(instructor.get().getUser());
                }
            }
        }

        newExtension.setReviewerResponse(extensionDTO.getResponseToRequester());
        newExtension.setReviewerResponseTimestamp(extensionDTO.getResponseTimestamp());
        return extensionRepo.save(newExtension);
    }


    public LateRequest createLateRequest(String courseId,
                                         User user,
                                         LateRequestDTO.RequestTypeEnum requestType,
                                         double daysRequested,
                                         Instant submissionDate,
                                         String assignmentId,
                                         LateRequestDTO.StatusEnum status,
                                         ExtensionDTO extension) {
        LateRequest lateRequest = new LateRequest();
        lateRequest.setDaysRequested(daysRequested);
        lateRequest.setLateRequestType(LateRequestType.valueOf(requestType.name()));
        lateRequest.setStatus(LateRequestStatus.valueOf(status.name()));
        lateRequest.setSubmissionDate(submissionDate);
        if(extension != null) {
            Extension newExtension = createExtensionFromDTO(courseId, user, extension);
            lateRequest.setExtension(newExtension);
        }
        Optional<Assignment> foundAssignment = assignmentService.getAssignmentById(assignmentId);
        foundAssignment.ifPresent(lateRequest::setAssignment);
        lateRequest.setRequestingUser(user);
        return lateRequestRepo.save(lateRequest);
    }

    public Optional<LateRequest> getLateRequest(@Nullable UUID id){
        if (id == null){
            return Optional.empty();
        }
        return Optional.ofNullable(lateRequestRepo.getLateRequestById(id));
    }

    public void deleteLateRequest(LateRequest lateRequest) {
        lateRequestRepo.delete(lateRequest);
    }

    public Map<String, LateRequest> getLateRequestsForAssignment(UUID assignment) {
        return lateRequestRepo.getLateRequestsForAssignment(assignment).collect(Collectors.toUnmodifiableMap(l -> l.getRequestingUser().getCwid(), l->l));
    }
}
