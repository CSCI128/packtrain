package edu.mines.packtrain.services;

import edu.mines.packtrain.data.ExtensionDTO;
import edu.mines.packtrain.data.LateRequestDTO;
import edu.mines.packtrain.models.*;
import edu.mines.packtrain.models.enums.LateRequestStatus;
import edu.mines.packtrain.models.enums.LateRequestType;
import edu.mines.packtrain.repositories.ExtensionRepo;
import edu.mines.packtrain.repositories.LateRequestRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
        if (lateRequestStatus == null) {
            return lateRequestRepo.getAllLateRequests(UUID.fromString(courseId));
        } else if (lateRequestStatus.equalsIgnoreCase("approved")) {
            return lateRequestRepo.getAllLateRequests(UUID.fromString(courseId)).stream().filter(c -> c.getStatus() == LateRequestStatus.APPROVED).toList();
        } else if (lateRequestStatus.equalsIgnoreCase("denied")) {
            return lateRequestRepo.getAllLateRequests(UUID.fromString(courseId)).stream().filter(c -> c.getStatus() == LateRequestStatus.REJECTED || c.getStatus() == LateRequestStatus.IGNORED).toList();
        } else if (lateRequestStatus.equalsIgnoreCase("pending")) {
            return lateRequestRepo.getAllLateRequests(UUID.fromString(courseId)).stream().filter(c -> c.getStatus() == LateRequestStatus.PENDING).toList();
        }
        return List.of();
    }

    public LateRequest approveExtension(String assignmentId, String userId, String extensionId, String reason) {
        Optional<LateRequest> lateRequest = getLateRequest(UUID.fromString(extensionId));

        if (lateRequest.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Late request '%s' for user '%s' does not exist", extensionId, userId));
        }

        lateRequest.get().setStatus(LateRequestStatus.APPROVED);
        lateRequest.get().getExtension().setReviewerResponse(reason);
        lateRequest.get().getExtension().setReviewerResponseTimestamp(Instant.now());

        extensionRepo.save(lateRequest.get().getExtension());

        return lateRequestRepo.save(lateRequest.get());
    }

    public LateRequest denyExtension(String assignmentId, String userId, String extensionId, String reason) {
        Optional<LateRequest> lateRequest = getLateRequest(UUID.fromString(extensionId));

        if (lateRequest.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Late request '%s' for user '%s' does not exist", extensionId, userId));
        }


        lateRequest.get().getExtension().setReviewerResponse(reason);
        lateRequest.get().getExtension().setReviewerResponseTimestamp(Instant.now());
        lateRequest.get().setStatus(LateRequestStatus.REJECTED);

        extensionRepo.save(lateRequest.get().getExtension());
        return lateRequestRepo.save(lateRequest.get());
    }

    public List<LateRequest> getAllLateRequestsForStudent(String courseId, User user) {
        return extensionRepo.getAllLateRequestsForStudent(UUID.fromString(courseId), user);
    }

    public Extension createExtensionFromDTO(String courseId, User user, ExtensionDTO extensionDTO) {
        Extension newExtension = new Extension();
        newExtension.setReason(extensionDTO.getReason());
        newExtension.setComments(extensionDTO.getComments());

        Course course = courseService.getCourse(UUID.fromString(courseId));

        Optional<Section> userSection = courseMemberService.getSectionsForUserAndCourse(user, course).stream().findFirst();

        CourseMember instructor = courseMemberService.getStudentInstructor(course, userSection);

        newExtension.setReviewer(instructor.getUser());

        newExtension.setReviewerResponse(extensionDTO.getResponseToRequester());
        newExtension.setReviewerResponseTimestamp(extensionDTO.getResponseTimestamp());

        return extensionRepo.save(newExtension);
    }


    public LateRequest createLateRequest(String courseId,
                                         User actingUser,
                                         LateRequestDTO.RequestTypeEnum requestType,
                                         double daysRequested,
                                         Instant submissionDate,
                                         String assignmentId,
                                         @Nullable ExtensionDTO extension) {
        LateRequest lateRequest = new LateRequest();
        lateRequest.setDaysRequested(daysRequested);
        lateRequest.setLateRequestType(LateRequestType.valueOf(requestType.name()));

        if (lateRequest.getLateRequestType() == LateRequestType.LATE_PASS) {
            lateRequest.setStatus(LateRequestStatus.APPROVED);
        } else {
            lateRequest.setStatus(LateRequestStatus.PENDING);
        }

        lateRequest.setSubmissionDate(submissionDate);

        if (extension != null) {
            Extension newExtension = createExtensionFromDTO(courseId, actingUser, extension);
            lateRequest.setExtension(newExtension);
        }

        Assignment assignment = assignmentService.getAssignmentById(assignmentId);
        lateRequest.setAssignment(assignment);
        lateRequest.setRequestingUser(actingUser);

        return lateRequestRepo.save(lateRequest);
    }

    public Optional<LateRequest> getLateRequest(@Nullable UUID id) {
        if (id == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(lateRequestRepo.getLateRequestById(id));
    }

    public void deleteLateRequest(Course course, User actingUser, String lateRequestId) {
        LateRequest lateRequest = getLateRequest(UUID.fromString(lateRequestId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Late request '%s' was not found!", lateRequestId)));

        if(lateRequest.getLateRequestType() == LateRequestType.LATE_PASS) {
            courseMemberService.refundLatePasses(course, actingUser, lateRequest.getDaysRequested());
        }

        lateRequestRepo.delete(lateRequest);
    }

    public Map<String, LateRequest> getLateRequestsForAssignment(UUID assignment) {
        return lateRequestRepo.getLateRequestsForAssignment(assignment).stream().collect(Collectors.toUnmodifiableMap(l -> l.getRequestingUser().getCwid(), l -> l));
    }
}
