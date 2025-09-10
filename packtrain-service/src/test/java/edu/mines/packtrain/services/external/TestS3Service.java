package edu.mines.packtrain.services.external;

import edu.mines.packtrain.containers.MinioTestContainer;
import edu.mines.packtrain.containers.PostgresTestContainer;
import edu.mines.packtrain.models.User;
import io.minio.*;
import io.minio.messages.Bucket;
import io.minio.messages.Item;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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
                .endpoint(minio.getS3URL())
                .credentials(minio.getUserName(), minio.getPassword())
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

        List<String> buckets = client.listBuckets().stream().map(Bucket::name).toList();

        Assertions.assertTrue(buckets.contains(bucketId.get()));

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

        Optional<String> resourceURI = s3Service.uploadPolicy(user, id, filename, file);

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
