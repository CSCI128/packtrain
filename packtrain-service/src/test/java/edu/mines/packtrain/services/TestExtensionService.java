package edu.mines.packtrain.services;

import edu.mines.packtrain.containers.PostgresTestContainer;
import edu.mines.packtrain.models.LateRequest;
import edu.mines.packtrain.models.enums.LateRequestStatus;
import edu.mines.packtrain.repositories.ExtensionRepo;
import edu.mines.packtrain.repositories.LateRequestRepo;
import edu.mines.packtrain.seeders.CourseSeeders;

import static org.mockito.ArgumentMatchers.any;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class TestExtensionService implements PostgresTestContainer {

    @Autowired ExtensionService extensionService;

    @Autowired
    private ExtensionRepo extensionRepo;
    @Autowired
    private CourseSeeders courseSeeders;
    @Autowired
    private LateRequestRepo lateRequestRepo;

    @BeforeAll
    static void setupClass(){
        postgres.start();
    }


    @AfterEach
    void tearDown(){
        extensionRepo.deleteAll();
    }

    // TO-DO: test that getAllExtensions does what is expected
    // Will need to create a migration seed in order to write this test
    // This test should be implemented when the ExtensionService is implemented
    @Test
    void verifyGetAllExtensions(){


    }

    @Test
    void verifyProcessExtensionApplied(){
        LateRequest mockLateRequest = new LateRequest();
        mockLateRequest.setDaysRequested(4);
        mockLateRequest.setStatus(LateRequestStatus.IGNORED);
        LateRequestRepo  mockRepo = Mockito.mock(LateRequestRepo.class);
        Mockito.when(mockRepo.getLateRequestById(any(UUID.class))).thenReturn(mockLateRequest);
        Mockito.when(mockRepo.save(any())).thenReturn(null);

        extensionService.processExtensionApplied(UUID.randomUUID(), true, 2);
        ArgumentCaptor<LateRequest> requestCaptor = ArgumentCaptor.forClass(LateRequest.class);
        Mockito.verify(mockRepo).save(requestCaptor.capture());

        LateRequest capturedRequest = requestCaptor.getValue();
        Assertions.assertEquals(capturedRequest.getStatus(), LateRequestStatus.APPLIED);
        Assertions.assertEquals(capturedRequest.getDaysRequested(), 2);

    }

}
