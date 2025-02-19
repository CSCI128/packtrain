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

    Supplier<Object> S3_URI = minio::getS3URL;
    Supplier<Object> ACCESS_KEY = minio::getUserName;
    Supplier<Object> SECRET_KEY = minio::getPassword;

    @DynamicPropertySource
    static void setMinioProperties(DynamicPropertyRegistry registry){
        registry.add("grading-admin.external-services.s3.uri", S3_URI);
        registry.add("grading-admin.external-services.s3.access_key", ACCESS_KEY);
        registry.add("grading-admin.external-services.s3.secret_key", SECRET_KEY);

    }
}
