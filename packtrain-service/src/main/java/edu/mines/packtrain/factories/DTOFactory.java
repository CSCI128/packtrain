package edu.mines.packtrain.factories;

import edu.mines.packtrain.data.AssignmentDTO;
import edu.mines.packtrain.data.AssignmentSlimDTO;
import edu.mines.packtrain.data.CourseDTO;
import edu.mines.packtrain.data.CourseLateRequestConfigDTO;
import edu.mines.packtrain.data.CourseMemberDTO;
import edu.mines.packtrain.data.CredentialDTO;
import edu.mines.packtrain.data.ExtensionDTO;
import edu.mines.packtrain.data.LateRequestDTO;
import edu.mines.packtrain.data.MasterMigrationDTO;
import edu.mines.packtrain.data.MasterMigrationStatisticsDTO;
import edu.mines.packtrain.data.MigrationDTO;
import edu.mines.packtrain.data.PolicyDTO;
import edu.mines.packtrain.data.TaskDTO;
import edu.mines.packtrain.data.UserDTO;
import edu.mines.packtrain.models.Assignment;
import edu.mines.packtrain.models.Course;
import edu.mines.packtrain.models.CourseLateRequestConfig;
import edu.mines.packtrain.models.CourseMember;
import edu.mines.packtrain.models.Credential;
import edu.mines.packtrain.models.LateRequest;
import edu.mines.packtrain.models.MasterMigration;
import edu.mines.packtrain.models.MasterMigrationStats;
import edu.mines.packtrain.models.Migration;
import edu.mines.packtrain.models.Policy;
import edu.mines.packtrain.models.Section;
import edu.mines.packtrain.models.User;
import edu.mines.packtrain.models.tasks.ScheduledTaskDef;

public class DTOFactory {

    private DTOFactory() {
        throw new IllegalStateException("Utility class");
    }

    public static LateRequestDTO toDto(LateRequest lateRequest) {
        return new LateRequestDTO()
                .id(lateRequest.getId())
                .numDaysRequested(lateRequest.getDaysRequested())
                .requestType(LateRequestDTO.RequestTypeEnum.fromValue(lateRequest
                        .getLateRequestType().name().toLowerCase()))
                .status(LateRequestDTO.StatusEnum.fromValue(lateRequest.getStatus()
                        .name().toLowerCase()))
                .dateSubmitted(lateRequest.getSubmissionDate())
                .assignmentId(lateRequest.getAssignment().getId())
                .assignmentName(lateRequest.getAssignment().getName())
                .instructor(lateRequest.getInstructor())
                .requestor(lateRequest.getRequestor())
                .userReviewer(lateRequest.getExtension() != null
                        ? lateRequest.getExtension().getReviewer().getName() : null)
                .extension(lateRequest.getExtension() != null ?
                        new ExtensionDTO()
                                .id(lateRequest.getExtension().getId())
                                .reason(lateRequest.getExtension().getReason())
                                .comments(lateRequest.getExtension().getComments())
                                .responseToRequester(lateRequest.getExtension()
                                        .getReviewerResponse())
                                .userReviewer(lateRequest.getExtension().getReviewer().getName())
                                .responseTimestamp(lateRequest.getExtension()
                                        .getReviewerResponseTimestamp())
                        : null);
    }

    public static PolicyDTO toDto(Policy policy) {
        return new PolicyDTO()
                .id(policy.getId())
                .name(policy.getPolicyName())
                .description(policy.getDescription())
                .numberOfMigrations(policy.getNumberOfMigrations())
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
                .name(courseMember.getUser().getName())
                .canvasId(courseMember.getCanvasId())
                .courseRole(CourseMemberDTO.CourseRoleEnum.fromValue(courseMember.getRole()
                        .getRole()))
                .cwid(courseMember.getUser().getCwid())
                .sections(courseMember.getSections().stream().map(Section::getName).toList());
    }

    public static CourseDTO toDto(Course course) {
        CourseDTO courseDto = new CourseDTO()
                .id(course.getId())
                .term(course.getTerm())
                .enabled(course.isEnabled())
                .name(course.getName())
                .canvasId(course.getCanvasId())
                .lateRequestConfig(toDto(course.getLateRequestConfig()))
                .code(course.getCode());

        if (course.getGradescopeConfig() != null
                && course.getGradescopeConfig().getGradescopeId() != null) {
            courseDto.setGradescopeId(Long.parseLong(
                    course.getGradescopeConfig().getGradescopeId()));
        }

        return courseDto;
    }

    public static TaskDTO toDto(ScheduledTaskDef task) {
        return new TaskDTO()
                .id(task.getId())
                .name(task.getTaskName())
                .status(task.getStatus().toString())
                .message(task.getStatusText())
                .submittedTime(task.getSubmittedTime())
                .completedTime(task.getCompletedTime());
    }

    public static AssignmentDTO toDto(Assignment assignment) {
        AssignmentDTO dto = new AssignmentDTO()
                .id(assignment.getId())
                .name(assignment.getName())
                .canvasId(assignment.getCanvasId())
                .points(assignment.getPoints())
                .dueDate(assignment.getDueDate())
                .unlockDate(assignment.getUnlockDate())
                .category(assignment.getCategory())
                .groupAssignment(assignment.isGroupAssignment())
                .attentionRequired(assignment.isAttentionRequired())
                .enabled(assignment.isEnabled());

        if (assignment.getExternalAssignmentConfig() != null) {
            if (assignment.getExternalAssignmentConfig().getType() != null) {
                dto.externalService(assignment.getExternalAssignmentConfig().getType().name());
            }
            dto.externalPoints(assignment.getExternalAssignmentConfig().getExternalPoints());
        }
        return dto;
    }

    public static AssignmentSlimDTO toSlimDto(Assignment assignment) {
        return new AssignmentSlimDTO()
                .id(assignment.getId())
                .canvasId(assignment.getCanvasId())
                .name(assignment.getName())
                .points(assignment.getPoints())
                .dueDate(assignment.getDueDate())
                .unlockDate(assignment.getUnlockDate())
                .category(assignment.getCategory());
    }

    public static CourseLateRequestConfigDTO toDto(CourseLateRequestConfig lateRequestConfig) {
        if (lateRequestConfig == null) {
            return null;
        }
        return new CourseLateRequestConfigDTO()
                .latePassesEnabled(lateRequestConfig.isLatePassesEnabled())
                .enabledExtensionReasons(lateRequestConfig.getEnabledExtensionReasons())
                .totalLatePassesAllowed(lateRequestConfig.getTotalLatePassesAllowed())
                .latePassName(lateRequestConfig.getLatePassName());
    }

    public static MasterMigrationStatisticsDTO toDto(MasterMigrationStats stats) {
        return new MasterMigrationStatisticsDTO()
                .totalSubmission(stats.getTotalSubmissions())
                .lateRequests(stats.getLateRequests())
                .totalExtensions(stats.getTotalLatePasses())
                .totalLatePasses(stats.getTotalLatePasses())
                .unapprovedRequests(stats.getUnapprovedRequests());
    }

    public static MigrationDTO toDto(Migration migration) {
        // we are join fetching the assignment and policy so they will always be available
        MigrationDTO dto = new MigrationDTO()
                .id(migration.getId())
                .assignment(toDto(migration.getAssignment()));
        if (migration.getPolicy() != null) {
            dto.policy(toDto(migration.getPolicy()));
        }
        return dto;
    }

    public static MasterMigrationDTO toDto(MasterMigration masterMigration) {
        MasterMigrationDTO dto = new MasterMigrationDTO()
                .id(masterMigration.getId())
                .dateStarted(masterMigration.getDateStarted())
                .status(MasterMigrationDTO.StatusEnum.fromValue(masterMigration.getStatus()
                        .getStatus()))
                .migrator(toDto(masterMigration.getCreatedByUser()));

        if (masterMigration.getMigrations() != null && !masterMigration.getMigrations().isEmpty()) {
            dto.migrations(masterMigration.getMigrations().stream().map(DTOFactory::toDto)
                    .toList());
        }

        return dto;
    }
}
