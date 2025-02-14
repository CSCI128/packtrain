package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.containers.MinioTestContainer;
import edu.mines.gradingadmin.containers.PostgresTestContainer;
import edu.mines.gradingadmin.models.User;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Bucket;
import io.minio.messages.Item;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

@SpringBootTest
@Slf4j
public class TestS3Service implements MinioTestContainer, PostgresTestContainer {
    @Autowired
    private S3Service s3Service;

    private static MinioClient client;

    @BeforeAll
    static void setupClass(){
        minio.start();
        client = MinioClient.builder()
                .endpoint(S3_URI.get().toString())
                .credentials(ACCESS_KEY.get().toString(), SECRET_KEY.get().toString())
                .build();
    }

    @AfterEach
    @SneakyThrows
    void tearDown(){
        // we dont have a nice "clearAll" function :(
        List<Bucket> buckets = client.listBuckets();
        for (Bucket b : buckets) {
            try {
                Iterable<Result<Item>> objects = client.listObjects(ListObjectsArgs.builder().bucket(b.name()).build());
                for (Result<Item> i : objects){
                    client.removeObject(RemoveObjectArgs.builder().bucket(b.name()).object(i.get().objectName()).build());
                }
                client.removeBucket(RemoveBucketArgs.builder().bucket(b.name()).build());
            } catch (Exception e) {
                log.error("Failed to remove bucket", e);
                throw e;
            }
        }
    }

    @Test
    @SneakyThrows
    void verifyCreateBucket(){
        Optional<String> bucketId = s3Service.createNewBucketForCourse(UUID.randomUUID());

        Assertions.assertTrue(bucketId.isPresent());

        List<Bucket> buckets = client.listBuckets();
        Assertions.assertEquals(1, buckets.size());
    }

    @Test
    @SneakyThrows
    void verifyNoDuplicateBuckets(){
        UUID id = UUID.randomUUID();
        Optional<String> bucketId = s3Service.createNewBucketForCourse(id);

        Assertions.assertTrue(bucketId.isPresent());

        bucketId = s3Service.createNewBucketForCourse(id);
        Assertions.assertTrue(bucketId.isPresent());

        List<Bucket> buckets = client.listBuckets();
        Assertions.assertEquals(1, buckets.size());


    }

    @Test
    @SneakyThrows
    void verifyCreateResource(){
        UUID id = UUID.randomUUID();

        User user = Mockito.mock(User.class);
        Mockito.when(user.getEmail()).thenReturn("user@test.com");

        Optional<String> bucketId = s3Service.createNewBucketForCourse(id);

        String expectedContent = "// this is valid js";

        String filename = "file.js";
        MockMultipartFile file = new MockMultipartFile(filename, expectedContent.getBytes());

        Optional<String> resourceURI = s3Service.uploadCourseWidePolicy(user, id, filename, file);

        Assertions.assertTrue(resourceURI.isPresent());

        URL url = new URL(resourceURI.get());
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

        String inputLine;

        StringBuilder content = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();

        Assertions.assertEquals(expectedContent, content.toString());
    }


}
