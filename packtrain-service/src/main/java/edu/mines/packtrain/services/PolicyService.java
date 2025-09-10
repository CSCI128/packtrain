package edu.mines.packtrain.services;

import edu.mines.packtrain.data.PolicyDryRunResultsDTO;
import edu.mines.packtrain.data.PolicyRawScoreDTO;
import edu.mines.packtrain.data.PolicyWithCodeDTO;
import edu.mines.packtrain.models.Course;
import edu.mines.packtrain.models.Policy;
import edu.mines.packtrain.models.User;
import edu.mines.packtrain.repositories.CourseRepo;
import edu.mines.packtrain.repositories.PolicyRepo;
import edu.mines.packtrain.services.external.PolicyServerService;
import edu.mines.packtrain.services.external.S3Service;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class PolicyService {
    private final S3Service s3Service;
    private final PolicyServerService policyServerService;
    private final CourseRepo courseRepo;
    private final PolicyRepo policyRepo;

    public PolicyService(S3Service s3Service, PolicyServerService policyServerService,
            CourseRepo courseRepo, PolicyRepo policyRepo) {
        this.s3Service = s3Service;
        this.policyServerService = policyServerService;
        this.courseRepo = courseRepo;
        this.policyRepo = policyRepo;
    }

    public Policy createNewPolicy(User actingUser, UUID courseId, String policyName,
            String description, String fileName, MultipartFile file) {
        // if this is slow, we may need to make this a task
        Optional<Course> course = courseRepo.getById(courseId);

        if (course.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course does not exist");
        }

        log.debug("Creating new course wide policy '{}' for course '{}'",
                policyName, course.get().getCode());

        Optional<String> policyUrl = s3Service.uploadPolicy(actingUser, courseId, fileName,
                file);

        if (policyUrl.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to upload policy");
        }

        // this should never happen, but if it does, then we also need to reject it as
        // the URIs
        // must be unique
        if (policyRepo.existsByPolicyURI(policyUrl.get())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A policy already exists " +
                    "with this URI");
        }

        Optional<String> validationError = policyServerService.validatePolicy(policyUrl.get());

        if (validationError.isPresent()) {
            log.error("Policy '{}' failed to validate due to: '{}'", policyName,
                    validationError.get());
            s3Service.deletePolicy(courseId, fileName);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Policy validation failure: "
                    + validationError.get());
        }

        Policy policy = new Policy();
        policy.setCourse(course.get());
        policy.setCreatedByUser(actingUser);
        policy.setPolicyName(policyName);
        policy.setDescription(description);
        policy.setFileName(fileName);
        policy.setPolicyURI(policyUrl.get());

        policy = policyRepo.save(policy);

        log.info("Created new policy '{}' for course '{}' at '{}'",
                policyName, course.get().getCode(), policyUrl.get());

        return policy;
    }

    public Optional<PolicyDryRunResultsDTO> dryRunPolicy(MultipartFile file,
            PolicyRawScoreDTO dto) {
        return policyServerService.dryRunPolicy(file, dto);
    }

    public Policy incrementUsedBy(Policy policy) {
        policy.setNumberOfMigrations(policy.getNumberOfMigrations() + 1);

        return policyRepo.save(policy);
    }

    public Policy decrementUsedBy(Policy policy) {
        if (policy.getNumberOfMigrations() == 0) {
            return policy;
        }

        policy.setNumberOfMigrations(policy.getNumberOfMigrations() - 1);
        return policyRepo.save(policy);
    }

    public boolean deletePolicy(UUID courseId, UUID policyId) {
        Optional<Policy> policy = policyRepo.getPolicyById(policyId);

        if (policy.isEmpty()) {
            log.warn("Attempt to get policy that doesn't exist!");
            return false;
        }

        if (policy.get().getNumberOfMigrations() != 0) {
            log.warn("Refusing to delete policy '{}' that is used in {} migrations!",
                    policy.get().getPolicyName(), policy.get().getNumberOfMigrations());
            return false;
        }

        if (!s3Service.deletePolicy(courseId, policy.get().getFileName())) {
            return false;
        }

        policyRepo.delete(policy.get());

        return true;
    }

    public List<Policy> getAllPolicies(UUID courseId) {
        Optional<Course> course = courseRepo.findById(courseId);

        if (course.isEmpty()) {
            return List.of();
        }

        return policyRepo.getPoliciesByCourse(course.get());
    }

    public Policy getPolicy(UUID policyId) {
        Optional<Policy> policy = policyRepo.getPolicyById(policyId);

        if (policy.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Policy does not exist");
        }

        return policy.get();
    }

    public PolicyWithCodeDTO getFullPolicy(UUID policyId) {
        Policy policy = getPolicy(policyId);

        Optional<Resource> policyText = s3Service.getPolicy(policy.getPolicyURI());

        if (policyText.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to download policy!");
        }

        return new PolicyWithCodeDTO()
                .filePath(policy.getFileName())
                .name(policy.getPolicyName())
                .description(policy.getDescription())
                .fileData(policyText.get());
    }

    public Policy updatePolicy(User actingUser, UUID policyId, UUID courseId, String policyName,
            String description, String fileName, MultipartFile file) {

        Policy policy = getPolicy(policyId);

        policy.setDescription(description);
        policy.setFileName(fileName);
        policy.setPolicyName(policyName);

        Optional<String> policyUrl = s3Service.uploadPolicy(actingUser, courseId, fileName, file, true);

        if (policyUrl.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to upload policy");
        }

        Optional<String> validationError = policyServerService.validatePolicy(policyUrl.get());

        if (validationError.isPresent()) {
            log.error("Policy '{}' failed to validate due to: '{}'", policyName,
                    validationError.get());
            s3Service.deletePolicy(courseId, fileName);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Policy validation failure: "
                    + validationError.get());
        }

        policy.setPolicyURI(policyUrl.get());

        return policyRepo.save(policy);
    }

}
