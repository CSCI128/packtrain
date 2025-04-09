package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.models.Course;
import edu.mines.gradingadmin.models.Policy;
import edu.mines.gradingadmin.models.User;
import edu.mines.gradingadmin.repositories.CourseRepo;
import edu.mines.gradingadmin.repositories.PolicyRepo;
import edu.mines.gradingadmin.services.external.PolicyServerService;
import edu.mines.gradingadmin.services.external.S3Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class PolicyService {
    private final S3Service s3Service;
    private final PolicyServerService policyServerService;
    private final CourseRepo courseRepo;
    private final PolicyRepo policyRepo;

    public PolicyService(S3Service s3Service, PolicyServerService policyServerService, CourseRepo courseRepo, PolicyRepo policyRepo) {
        this.s3Service = s3Service;
        this.policyServerService = policyServerService;
        this.courseRepo = courseRepo;
        this.policyRepo = policyRepo;
    }


    public Optional<Policy> createNewPolicy(User actingUser, UUID courseId, String policyName, String description, String fileName, MultipartFile file){
        // if this is slow, we may need to make this a task
        Optional<Course> course = courseRepo.findById(courseId);

        if (course.isEmpty()){
            log.warn("Course '{}' does not exist!", courseId);
            return Optional.empty();
        }

        log.debug("Creating new course wide policy '{}' for course '{}'", policyName, course.get().getCode());

        Optional<String> policyUrl = s3Service.uploadNewPolicy(actingUser, courseId, fileName, file);

        if (policyUrl.isEmpty()){
            log.warn("Failed to upload policy '{}'", policyName);
            return Optional.empty();
        }

        // this should never happen, but if it does, then we also need to reject it as the URIs must be unique
        if (policyRepo.existsByPolicyURI(policyUrl.get())){
            log.warn("Policy already exists at url '{}'", policyUrl.get());
            return Optional.empty();
        }

        Optional<String> validationError = policyServerService.validatePolicy(policyUrl.get());

        if (validationError.isPresent()){
            log.error("Policy '{}' failed to validate due to: '{}'", policyName, validationError.get());
            s3Service.deletePolicy(courseId, fileName);
            return Optional.empty();
        }

        Policy policy = new Policy();
        policy.setCourse(course.get());
        policy.setCreatedByUser(actingUser);
        policy.setPolicyName(policyName);
        policy.setDescription(description);
        policy.setFileName(fileName);
        policy.setPolicyURI(policyUrl.get());

        policy = policyRepo.save(policy);

        log.info("Created new policy '{}' for course '{}' at '{}'", policyName, course.get().getCode(), policyUrl.get());

        return Optional.of(policy);
    }


    public Policy incrementUsedBy(Policy policy){
        policy.setNumberOfMigrations(policy.getNumberOfMigrations() + 1);

        return policyRepo.save(policy);
    }

    public Policy decrementUsedBy(Policy policy){
        if (policy.getNumberOfMigrations() == 0){
            return policy;
        }

        policy.setNumberOfMigrations(policy.getNumberOfMigrations() - 1);
        return policyRepo.save(policy);
    }

    public boolean deletePolicy(UUID courseId, UUID policyId){
        Optional<Policy> policy = policyRepo.getPolicyById(policyId);

        if (policy.isEmpty()){
            log.warn("Attempt to get policy that doesn't exist!");
            return false;
        }

        if (policy.get().getNumberOfMigrations() != 0){
            log.warn("Refusing to delete policy '{}' that is used in {} migrations!", policy.get().getPolicyName(), policy.get().getNumberOfMigrations());
            return false;
        }

        if(!s3Service.deletePolicy(courseId, policy.get().getFileName())){
            return false;
        }

        policyRepo.delete(policy.get());

        return true;
    }

    public List<Policy> getAllPolicies(UUID courseId){
        Optional<Course> course = courseRepo.findById(courseId);

        if (course.isEmpty()){
            return List.of();
        }

        return policyRepo.getPoliciesByCourse(course.get());
    }

    public Optional<Policy> getPolicy(URI policyURI){
        Optional<Policy> policy = policyRepo.getPolicyByURI(policyURI.toString());

        if (policy.isEmpty()){
            return Optional.empty();
        }

        return policy;
    }
}
