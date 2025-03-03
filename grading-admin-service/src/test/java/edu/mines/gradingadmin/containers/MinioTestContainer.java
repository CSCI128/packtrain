package edu.mines.gradingadmin.containers;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.function.Supplier;

@Testcontainers
public interface MinioTestContainer {
    MinIOContainer minio = new MinIOContainer(DockerImageName.parse("minio/minio"));

    @DynamicPropertySource
    static void setMinioProperties(DynamicPropertyRegistry registry){
        registry.add("grading-admin.external-services.s3.enabled", () -> true);
        registry.add("grading-admin.external-services.s3.uri", minio::getS3URL);
        registry.add("grading-admin.external-services.s3.access_key", minio::getUserName);
        registry.add("grading-admin.external-services.s3.secret_key", minio::getPassword);

    }
}
