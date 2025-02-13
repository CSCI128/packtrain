package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.config.EndpointConfig;
import edu.mines.gradingadmin.models.User;
import io.minio.*;
import io.minio.errors.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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

@Service
@Slf4j
public class S3Service {

    private final MinioClient s3Client;
    private final EndpointConfig.S3Config config;

    public S3Service(EndpointConfig.S3Config config) throws MalformedURLException {
        this.config = config;
        s3Client = MinioClient.builder()
                .endpoint(config.getEndpoint().toURL())
                .credentials(config.getAccessKey(), config.getSecretKey())
                .build();
    }


    private boolean bucketExists(String bucket){
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

    private boolean objectExists(String bucket, String objectName){
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
        } catch (MinioException | InvalidKeyException | IOException | NoSuchAlgorithmException e){
            return false;
        }
    }

    public Optional<String> createNewBucketForCourse(UUID courseId){
        if (bucketExists(courseId.toString())){
            return Optional.of(courseId.toString());
        }

        MakeBucketArgs args = MakeBucketArgs.builder().bucket(courseId.toString()).build();

        try{
            s3Client.makeBucket(args);

        }catch (MinioException | InvalidKeyException | IOException | NoSuchAlgorithmException e){
            log.error("Failed to create new bucket for course '{}'", courseId);
            log.error("Failed due to: ", e);
            return Optional.empty();
        }

        return Optional.of(courseId.toString());
    }

    public Optional<String> uploadCourseWidePolicy(User creatingUser, UUID courseId, String filename, MultipartFile policyDocument){
        if (!bucketExists(courseId.toString())){
            log.error("Failed to upload policy! Bucket for course '{}' does not exist!", courseId);
            return Optional.empty();
        }

        if (objectExists(courseId.toString(), filename)){
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

        } catch (IOException e){
            log.error("Failed to get policy document content", e);
            return Optional.empty();
        }
        catch (MinioException | InvalidKeyException | NoSuchAlgorithmException e){
            log.error("Failed to upload policy '{}' for course '{}'", filename, courseId);
            log.error("Failed due to: ", e);
            return Optional.empty();
        }

    }


}
