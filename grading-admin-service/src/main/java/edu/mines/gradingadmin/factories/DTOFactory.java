package edu.mines.gradingadmin.factories;

import edu.mines.gradingadmin.data.*;
import edu.mines.gradingadmin.models.*;
import edu.mines.gradingadmin.models.tasks.ScheduledTaskDef;

public class DTOFactory {

    public static LateRequestDTO toDto(LateRequest lateRequest) {
        return new LateRequestDTO()
            .id(lateRequest.getId().toString())
            .numDaysRequested(lateRequest.getDaysRequested())
            .requestType(LateRequestDTO.RequestTypeEnum.fromValue(lateRequest.getRequestType().name().toLowerCase()))
            .status(LateRequestDTO.StatusEnum.fromValue(lateRequest.getStatus().name().toLowerCase()))
            .dateSubmitted(lateRequest.getSubmissionDate())
            .assignmentId(lateRequest.getAssignment().getId().toString())
            .assignmentName(lateRequest.getAssignment().getName())
            .extension(lateRequest.getExtension() != null ?
                new ExtensionDTO()
                    .id(lateRequest.getExtension().getId().toString())
                    .reason(lateRequest.getExtension().getReason())
                    .comments(lateRequest.getExtension().getComments())
                    .responseToRequester(lateRequest.getExtension().getReviewerResponse())
                    .responseTimestamp(lateRequest.getExtension().getReviewerResponseTimestamp())
                : null);
    }

    public static PolicyDTO toDto(Policy policy) {
        return new PolicyDTO()
            .course(DTOFactory.toDto(policy.getCourse()))
            .name(policy.getPolicyName())
            .uri(policy.getPolicyURI());
    }

    public static CredentialDTO toDto(Credential credential) {
        return new CredentialDTO()
            .id(credential.getId().toString())
            .name(credential.getName())
            .service(CredentialDTO.ServiceEnum.valueOf(credential.getType().toString()))
            ._private(credential.isPrivate());
    }

    public static UserDTO toDto(User user) {
        return new UserDTO()
            .cwid(user.getCwid())
            .email(user.getEmail())
            .name(user.getName())
            .admin(user.isAdmin())
            .enabled(user.isEnabled());
    }

    public static CourseMemberDTO toDto(CourseMember courseMember) {
        return new CourseMemberDTO()
            .canvasId(courseMember.getCanvasId())
            .courseRole(CourseMemberDTO.CourseRoleEnum.fromValue(courseMember.getRole().getRole()))
            .cwid(courseMember.getUser().getCwid())
            .sections(courseMember.getSections().stream().map(Section::getName).toList());
    }

    public static CourseSlimDTO toSlimDto(Course course) {
        return new CourseSlimDTO()
            .id(course.getId().toString())
            .term(course.getTerm())
            .name(course.getName())
            .code(course.getCode());
    }

    public static CourseDTO toDto(Course course) {
        return new CourseDTO()
            .id(course.getId().toString())
            .term(course.getTerm())
            .enabled(course.isEnabled())
            .name(course.getName())
            .canvasId(course.getCanvasId())
            .code(course.getCode());
    }

    public static TaskDTO toDto(ScheduledTaskDef task) {
        return new TaskDTO()
            .id(task.getId())
            .name(task.getTaskName())
            .status(task.getStatus().toString())
            .submittedTime(task.getSubmittedTime());
    }

    public static AssignmentDTO toDto(Assignment assignment) {
        return new AssignmentDTO()
            .id(assignment.getId().toString())
            .name(assignment.getName())
            .canvasId(assignment.getCanvasId())
            .points(assignment.getPoints())
            .dueDate(assignment.getDueDate())
            .unlockDate(assignment.getUnlockDate())
            .category(assignment.getCategory())
            .groupAssignment(assignment.isGroupAssignment())
            .attentionRequired(assignment.isAttentionRequired())
            // TODO need to add external source config
            .enabled(assignment.isEnabled());
    }

    public static AssignmentSlimDTO toSlimDto(Assignment assignment) {
        return new AssignmentSlimDTO()
            .id(assignment.getId().toString())
            .name(assignment.getName())
            .points(assignment.getPoints())
            .dueDate(assignment.getDueDate())
            .unlockDate(assignment.getUnlockDate())
            .category(assignment.getCategory());
    }
}
