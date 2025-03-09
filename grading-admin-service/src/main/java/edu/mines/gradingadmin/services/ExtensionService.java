package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.data.AssignmentDTO;
import edu.mines.gradingadmin.data.CourseMemberDTO;
import edu.mines.gradingadmin.data.ExtensionDTO;
import edu.mines.gradingadmin.data.LateRequestDTO;
import edu.mines.gradingadmin.models.Extension;
import edu.mines.gradingadmin.models.LateRequest;
import edu.mines.gradingadmin.models.User;
import edu.mines.gradingadmin.repositories.ExtensionRepo;
import edu.mines.gradingadmin.repositories.LateRequestRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class ExtensionService {
    private final ExtensionRepo extensionRepo;
    private final LateRequestRepo lateRequestRepo;

    public ExtensionService(ExtensionRepo extensionRepo, LateRequestRepo lateRequestRepo) {
        this.extensionRepo = extensionRepo;
        this.lateRequestRepo = lateRequestRepo;
    }

    public List<Extension> getExtensionsByMigrationId(String migrationId) {
        return extensionRepo.getExtensionsByMigrationId(UUID.fromString(migrationId));
    }

    public List<LateRequest> getAllLateRequestsForStudent(String courseId, User user) {
        return extensionRepo.getAllLateRequestsForStudent(UUID.fromString(courseId), user);
    }

    public LateRequest createLateRequest(LateRequestDTO.RequestTypeEnum requestType,
                                         CourseMemberDTO requester,
                                         int daysRequested,
                                         List<AssignmentDTO> assignments,
                                         LateRequestDTO.StatusEnum status,
                                         ExtensionDTO extension) {
//        if extension isnt null, save it first
//        extensionRepo.save()

        LateRequest lateRequest = new LateRequest();

        return lateRequestRepo.save(lateRequest);
    }
}
