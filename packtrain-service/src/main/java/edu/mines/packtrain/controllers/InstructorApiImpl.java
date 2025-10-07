package edu.mines.packtrain.controllers;

import static java.util.stream.Collectors.toList;
import edu.mines.packtrain.api.InstructorApiDelegate;
import edu.mines.packtrain.data.AssignmentDTO;
import edu.mines.packtrain.data.CourseDTO;
import edu.mines.packtrain.data.CourseMemberDTO;
import edu.mines.packtrain.data.ErrorResponseDTO;
import edu.mines.packtrain.data.LateRequestDTO;
import edu.mines.packtrain.data.MasterMigrationDTO;
import edu.mines.packtrain.data.MigrationScoreChangeDTO;
import edu.mines.packtrain.data.MigrationWithScoresDTO;
import edu.mines.packtrain.data.PolicyDTO;
import edu.mines.packtrain.data.TaskDTO;
import edu.mines.packtrain.factories.DTOFactory;
import edu.mines.packtrain.managers.SecurityManager;
import edu.mines.packtrain.models.Course;
import edu.mines.packtrain.models.CourseMember;
import edu.mines.packtrain.models.LateRequest;
import edu.mines.packtrain.models.MasterMigration;
import edu.mines.packtrain.models.Migration;
import edu.mines.packtrain.models.Policy;
import edu.mines.packtrain.models.Section;
import edu.mines.packtrain.models.enums.CourseRole;
import edu.mines.packtrain.models.tasks.ScheduledTaskDef;
import edu.mines.packtrain.services.AssignmentService;
import edu.mines.packtrain.services.CourseMemberService;
import edu.mines.packtrain.services.CourseService;
import edu.mines.packtrain.services.ExtensionService;
import edu.mines.packtrain.services.MigrationService;
import edu.mines.packtrain.services.PolicyService;
import edu.mines.packtrain.services.RawScoreService;
import edu.mines.packtrain.services.SectionService;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequiredArgsConstructor
public class InstructorApiImpl implements InstructorApiDelegate {

    private final CourseService courseService;
    private final SectionService sectionService;
    private final ExtensionService extensionService;
    private final CourseMemberService courseMemberService;
    private final SecurityManager securityManager;
    private final MigrationService migrationService;
    private final PolicyService policyService;
    private final RawScoreService rawScoreService;
    private final AssignmentService assignmentService;

    @Override
    public ResponseEntity<List<MigrationWithScoresDTO>> getMasterMigrationToReview(UUID courseId,
                                                                       UUID masterMigrationId) {
        return ResponseEntity.ok(migrationService.getMasterMigrationToReview(masterMigrationId));
    }

    @Override
    public ResponseEntity<List<TaskDTO>> applyMasterMigration(UUID courseId,
                                                              UUID masterMigrationId) {
        List<ScheduledTaskDef> tasks = migrationService.startProcessScoresAndExtensions(
                securityManager.getUser(), masterMigrationId);

        return ResponseEntity.accepted().body(tasks.stream().map(DTOFactory::toDto).toList());
    }

    @Override
    public ResponseEntity<Void> applyValidateMasterMigration(UUID courseId,
                                                             UUID masterMigrationId) {
        boolean b = migrationService.validateApplyMasterMigration(masterMigrationId);

        if (!b) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<MasterMigrationDTO> createMasterMigration(UUID courseId) {
        MasterMigration masterMigration = migrationService.createMasterMigration(courseId,
                securityManager.getUser());

        return ResponseEntity.status(HttpStatus.CREATED).body(DTOFactory.toDto(masterMigration));

    }

    @Override
    public ResponseEntity<Void> deleteMasterMigration(UUID courseId, UUID masterMigrationId) {
        if (!migrationService.deleteMasterMigration(courseId, masterMigrationId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to delete " +
                    "master migration!");
        }

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Override
    public ResponseEntity<MasterMigrationDTO> createMigrationForMasterMigration(
            UUID courseId, UUID masterMigrationId, UUID assignmentId) {
        MasterMigration masterMigration = migrationService.addMigration(masterMigrationId,
                assignmentId);

        return ResponseEntity.status(HttpStatus.CREATED).body(DTOFactory.toDto(masterMigration));
    }

    @Override
    public ResponseEntity<List<LateRequestDTO>> getAllExtensionsForCourse(UUID courseId,
                                                                          String status) {
        List<LateRequest> lateRequests = extensionService.getAllLateRequests(courseId, status);

        return ResponseEntity.ok(lateRequests.stream().map(DTOFactory::toDto).toList());
    }

    @Override
    public ResponseEntity<List<ErrorResponseDTO>> getAllApprovedExtensionsForAssignment(
            UUID courseId, UUID assignmentId, UUID extensionId, String status) {
        return InstructorApiDelegate.super.getAllApprovedExtensionsForAssignment(courseId,
                assignmentId, extensionId, status);
    }

    @Override
    public ResponseEntity<List<ErrorResponseDTO>> getAllApprovedExtensionsForMember(
            UUID courseId, String cwid, UUID extensionId, String status) {
        return InstructorApiDelegate.super.getAllApprovedExtensionsForMember(courseId, cwid,
                extensionId, status);
    }

    @Override
    public ResponseEntity<List<ErrorResponseDTO>> getAllExtensionsForSection(
            UUID courseId, String sectionId, UUID extensionId, String status) {
        return InstructorApiDelegate.super.getAllExtensionsForSection(courseId, sectionId,
                extensionId, status);
    }

    @Override
    public ResponseEntity<List<MasterMigrationDTO>> getAllMigrations(UUID courseId) {
        List<MasterMigration> masterMigrations = migrationService.getAllMasterMigrations(courseId);

        return ResponseEntity.ok(masterMigrations.stream().map(DTOFactory::toDto).toList());
    }

    @Override
    public ResponseEntity<MasterMigrationDTO> getMasterMigration(UUID courseId,
                                                                 UUID masterMigrationId) {
        MasterMigration masterMigration = migrationService.getMasterMigration(masterMigrationId);

        return ResponseEntity.ok(DTOFactory.toDto(masterMigration));
    }

    @Override
    public ResponseEntity<Void> loadMasterMigration(UUID courseId, UUID masterMigrationId) {
        boolean res = migrationService.finalizeLoadMasterMigration(masterMigrationId);

        if (!res) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to finalize load " +
                    "phase for migration!");
        }

        return ResponseEntity.accepted().build();
    }

    @Override
    public ResponseEntity<Void> loadValidateMasterMigration(UUID courseId, UUID masterMigrationId) {
        boolean res = migrationService.validateLoadMasterMigration(masterMigrationId);

        if (!res) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to validate load " +
                    "phase for migration!");
        }

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<List<TaskDTO>> postMasterMigration(UUID courseId,
                                                             UUID masterMigrationId) {
        return ResponseEntity.accepted().body(migrationService.processMigrationLog(
                securityManager.getUser(), masterMigrationId).stream().map(DTOFactory::toDto)
                .toList());
    }

    @Override
    public ResponseEntity<Void> reviewMasterMigration(UUID courseId, UUID masterMigrationId) {
        boolean res = migrationService.finalizeReviewMasterMigration(masterMigrationId);

        if (!res) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to finalize " +
                    "review phase for migration!");
        }

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<MasterMigrationDTO> setPolicy(UUID courseId, UUID masterMigrationId,
                                                        UUID migrationId, UUID policyId) {
        Migration newMigration = migrationService.setPolicyForMigration(migrationId, policyId);

        MasterMigration masterMigration = migrationService.getMasterMigration(masterMigrationId);

        return ResponseEntity.accepted().body(DTOFactory.toDto(masterMigration));
    }

    @Override
    public ResponseEntity<Void> updateStudentScore(UUID courseId, UUID masterMigrationId,
                                               UUID migrationId,
                                               MigrationScoreChangeDTO migrationScoreChangeDTO) {
        boolean res = migrationService.updateStudentScore(securityManager.getUser(),
                migrationId, migrationScoreChangeDTO);

        if (!res) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to update score " +
                    "for student!");
        }

        return ResponseEntity.accepted().build();
    }

    @Override
    public ResponseEntity<MasterMigrationDTO> uploadRawScores(UUID courseId,
                                                              UUID masterMigrationId,
                                                              UUID migrationId, Resource body) {
        try {
            switch (migrationService.getAssignmentForMigration(migrationId)
                    .getExternalAssignmentConfig().getType()) {
                case GRADESCOPE ->
                        rawScoreService.uploadGradescopeCSV(body.getInputStream(), migrationId);
                case PRAIRIELEARN ->
                        rawScoreService.uploadPrairieLearnCSV(body.getInputStream(), migrationId);
                case RUNESTONE ->
                        rawScoreService.uploadRunestoneCSV(body.getInputStream(), migrationId);
                default -> throw new IllegalStateException("Request to import non supported csv");
            }
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }

        MasterMigration masterMigration = migrationService.getMasterMigration(masterMigrationId);

        return ResponseEntity.accepted().body(DTOFactory.toDto(masterMigration));
    }

    @Override
    @Transactional
    public ResponseEntity<CourseDTO> getCourseInformationInstructor(UUID courseId) {
        Course course = courseService.getCourse(courseId);


        Set<Section> sections = courseMemberService.getSectionsForUserAndCourse(
                securityManager.getUser(), course);

        CourseDTO courseDTO = DTOFactory.toDto(course)
                .assignments(course.getAssignments().stream().map(DTOFactory::toDto).toList())
                .members(sections.stream().map(Section::getMembers).flatMap(Set::stream)
                        .map(DTOFactory::toDto).collect(toList()))
                .sections(sections.stream().map(Section::getName).toList());

        return ResponseEntity.ok(courseDTO);
    }

    @Override
    public ResponseEntity<List<CourseMemberDTO>> getMembersInstructor(UUID courseId,
                                                                      List<String> enrollments,
                                                                      String name, String cwid) {
        Course course = courseService.getCourse(courseId);

        if (name != null && cwid != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Must define one of " +
                    "'name' or 'cwid'");
        }

        List<CourseRole> roles = new ArrayList<>();
        if (enrollments == null) {
            roles = List.of(CourseRole.values());
        } else {
            if (enrollments.contains("tas")) {
                roles.add(CourseRole.TA);
            }

            if (enrollments.contains("instructors")) {
                roles.add(CourseRole.INSTRUCTOR);
                roles.add(CourseRole.OWNER);
            }

            if (enrollments.contains("students")) {
                roles.add(CourseRole.STUDENT);
            }
        }
        List<CourseMember> members = courseMemberService.searchCourseMembers(course, roles,
                name, cwid);
        members.forEach(m -> m.setSections(sectionService.getSectionByMember(m)));

        return ResponseEntity.ok(members
                .stream().map(DTOFactory::toDto).toList());
    }

    @Override
    public ResponseEntity<List<CourseMemberDTO>> getInstructorEnrollments(UUID courseId,
                                                                          String name,
                                                                          String cwid) {
        Course course = courseService.getCourse(courseId);

        if (name != null && cwid != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Must define one " +
                    "of 'name' or 'cwid'");
        }

        return ResponseEntity.ok(courseMemberService.searchCourseMembers(course, List.of(),
                        name, cwid)
                .stream().map(DTOFactory::toDto).toList());
    }

    @Override
    @Transactional
    public ResponseEntity<LateRequestDTO> approveExtension(UUID courseId, UUID extensionId, String reason) {
        LateRequest lateRequest = extensionService.approveExtension(extensionId, reason);
        return ResponseEntity.accepted().body(DTOFactory.toDto(lateRequest));
    }

    @Override
    @Transactional
    public ResponseEntity<LateRequestDTO> denyExtension(UUID courseId, UUID extensionId, String reason) {
        LateRequest lateRequest = extensionService.denyExtension(extensionId, reason);
        return ResponseEntity.accepted().body(DTOFactory.toDto(lateRequest));
    }

    @Override
    public ResponseEntity<List<PolicyDTO>> getAllPolicies(UUID courseId) {
        List<Policy> policies = policyService.getAllPolicies(courseId);
        return ResponseEntity.ok(policies.stream().map(DTOFactory::toDto).toList());
    }

    @Override
    public ResponseEntity<Void> finalizeMasterMigration(UUID courseId, UUID masterMigrationId) {
        boolean res = migrationService.finalizePostToCanvas(masterMigrationId);

        if (!res) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to finalize " +
                    "migration!");
        }

        return ResponseEntity.accepted().build();
    }

    @Override
    public ResponseEntity<List<AssignmentDTO>> getCourseAssignmentsInstuctor(UUID courseId,
                                                                         Boolean onlyMigratable) {
        if (onlyMigratable == null) {
            onlyMigratable = false;
        }

        Course course = courseService.getCourse(courseId);

        return ResponseEntity.ok(assignmentService
                .getAllAssignmentsGivenCourse(course, onlyMigratable)
                .stream()
                .map(DTOFactory::toDto)
                .toList());
    }
}

