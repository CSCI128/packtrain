package edu.mines.packtrain.services.external;

import edu.mines.packtrain.config.ExternalServiceConfig;
import edu.mines.packtrain.models.User;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketPolicyArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.MinioException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class S3Service {

    private MinioClient s3Client;
    private final ExternalServiceConfig.S3Config config;

    public S3Service(ExternalServiceConfig.S3Config config) throws MalformedURLException {
        this.config = config;

        if (!config.isEnabled()) {
            return;
        }
        s3Client = MinioClient.builder()
                .endpoint(config.getEndpoint().toURL())
                .credentials(config.getAccessKey(), config.getSecretKey())
                .build();
    }


    private boolean bucketExists(String bucket) {
        if (!config.isEnabled()) {
            throw new ExternalServiceDisabledException("S3 Service is disabled!");
        }
        BucketExistsArgs args = BucketExistsArgs.builder()
                .bucket(bucket)
                .build();

        try {
            return s3Client.bucketExists(args);
        } catch (MinioException | InvalidKeyException | IOException | NoSuchAlgorithmException e) {
            log.error("Failed to check if bucket exists!", e);
            return false;
        }

    }

    private boolean objectExists(String bucket, String objectName) {
        if (!config.isEnabled()) {
            throw new ExternalServiceDisabledException("S3 Service is disabled!");
        }
        StatObjectArgs args = StatObjectArgs.builder()
                .bucket(bucket)
                .object(objectName)
                .build();

        try {
            // hacky garbage to check if an object exists.
            // basically there's not an object exists method,
            // and this throws an exception if it cant find the object
            s3Client.statObject(args);
            return true;
        } catch (MinioException | InvalidKeyException | IOException | NoSuchAlgorithmException e) {
            return false;
        }
    }

    public Optional<String> createNewBucketForCourse(UUID courseId) {
        if (!config.isEnabled()) {
            throw new ExternalServiceDisabledException("S3 Service is disabled!");
        }
        if (bucketExists(courseId.toString())) {
            return Optional.of(courseId.toString());
        }

        // Allows you to download any resources in this bucket.
        // This is an acceptable policy for this as it is internal to the docker network,
        // but if this were exposed, we'd want to adjust this.
        // Generated with https://awspolicygen.s3.amazonaws.com/policygen.html
        String policy = String.format("""
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Action": [
                        "s3:GetObject"
                      ],
                      "Effect": "Allow",
                      "Resource": "arn:aws:s3:::%s/*",
                      "Principal": "*"
                    }
                  ]
                }""", courseId);

        MakeBucketArgs args = MakeBucketArgs.builder().bucket(courseId.toString()).build();
        SetBucketPolicyArgs policyArgs = SetBucketPolicyArgs.builder()
                .bucket(courseId.toString())
                .config(policy)
                .build();

        try {
            s3Client.makeBucket(args);
            s3Client.setBucketPolicy(policyArgs);

        } catch (MinioException | InvalidKeyException | IOException | NoSuchAlgorithmException e) {
            log.error("Failed to create new bucket for course '{}'", courseId);
            log.error("Failed due to: ", e);
            return Optional.empty();
        }

        return Optional.of(courseId.toString());
    }

    public Optional<String> uploadNewPolicy(User creatingUser, UUID courseId, String filename,
                                            MultipartFile policyDocument) {
        if (!config.isEnabled()) {
            throw new ExternalServiceDisabledException("S3 Service is disabled!");
        }
        if (!bucketExists(courseId.toString())) {
            log.warn("Bucket for course '{}' does not exist!", courseId);
            if (createNewBucketForCourse(courseId).isEmpty()) {
                log.error("Failed to create bucket for course '{}'! Failed to upload policy!",
                        courseId);
                return Optional.empty();
            }
        }

        if (objectExists(courseId.toString(), filename)) {
            log.warn("Refusing to upload policy '{}'! Policy already exists!", filename);
            return Optional.empty();
        }

        log.debug("Uploading course wide policy '{}' for course '{}'.", filename, courseId);

        try {
            Map<String, String> metadata = Map.of(
                    "course-id", courseId.toString(),
                    "policy-type", "global",
                    "created-by", creatingUser.getEmail(),
                    "created-on", LocalDateTime.now().toString()
            );

            InputStream docStream = policyDocument.getInputStream();

            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(courseId.toString())
                    .object(filename)
                    .stream(docStream, -1, 10485760)
                    .contentType("text/javascript")
                    .tags(metadata)
                    .build();

            ObjectWriteResponse res = s3Client.putObject(args);

            log.debug("Upload for policy '{}' succeeded for course '{}'", filename, courseId);

            return Optional.of(
                    new StringJoiner("/")
                            .add(config.getEndpoint().toString())
                            .add(courseId.toString())
                            .add(res.object()).toString());

        } catch (IOException e) {
            log.error("Failed to get policy document content", e);
            return Optional.empty();
        } catch (MinioException | InvalidKeyException | NoSuchAlgorithmException e) {
            log.error("Failed to upload policy '{}' for course '{}'", filename, courseId);
            log.error("Failed due to: ", e);
            return Optional.empty();
        }

    }

    public boolean deletePolicy(UUID courseId, String filename) {
        if (!config.isEnabled()) {
            throw new ExternalServiceDisabledException("S3 Service is disabled!");
        }
        if (!bucketExists(courseId.toString())) {
            log.error("Failed to delete policy! Bucket for course '{}' does not exist!", courseId);
            return false;
        }

        if (!objectExists(courseId.toString(), filename)) {
            return true;
        }

        try {
            RemoveObjectArgs args = RemoveObjectArgs.builder().bucket(courseId.toString()).object(filename).build();
            s3Client.removeObject(args);
        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            log.error("Failed to remove policy '{}' for course '{}'", filename, courseId);
            log.error("Failed due to: ", e);
            return false;
        }

        return true;
    }


}
