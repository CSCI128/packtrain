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
    private final CourseMemberRepo courseMemberRepo;
    private final AssignmentService assignmentService;
    private final CourseMemberService courseMemberService;
    private final CourseService courseService;

    public ExtensionService(ExtensionRepo extensionRepo, LateRequestRepo lateRequestRepo, CourseMemberRepo courseMemberRepo, AssignmentService assignmentService, CourseMemberService courseMemberService, CourseService courseService) {
        this.extensionRepo = extensionRepo;
        this.lateRequestRepo = lateRequestRepo;
        this.courseMemberRepo = courseMemberRepo;
        this.assignmentService = assignmentService;
        this.courseMemberService = courseMemberService;
        this.courseService = courseService;
    }

    public List<Extension> getExtensionsByMigrationId(String migrationId) {
        return extensionRepo.getExtensionsByMigrationId(UUID.fromString(migrationId));
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

    public void useLatePasses(Course course, User user, Double amount) {
        Optional<CourseMember> courseMember = courseMemberRepo.findAllByCourseByCwid(course, user.getCwid()).stream().findFirst();
        if(courseMember.isPresent()) {
            double finalLatePasses = courseMember.get().getLatePassesUsed() + amount;
            courseMember.get().setLatePassesUsed(finalLatePasses);
            courseMemberRepo.save(courseMember.get());
        }
    }

    public void refundLatePasses(Course course, User user, Double amount) {
        Optional<CourseMember> courseMember = courseMemberRepo.findAllByCourseByCwid(course, user.getCwid()).stream().findFirst();
        if(courseMember.isPresent()) {
            double finalLatePasses = courseMember.get().getLatePassesUsed() - amount;
            courseMember.get().setLatePassesUsed(finalLatePasses);
            courseMemberRepo.save(courseMember.get());
        }
    }
}
