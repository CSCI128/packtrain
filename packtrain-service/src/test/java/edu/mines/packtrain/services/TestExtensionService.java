package edu.mines.packtrain.services;

import edu.mines.packtrain.containers.PostgresTestContainer;
import edu.mines.packtrain.models.LateRequest;
import edu.mines.packtrain.models.enums.LateRequestStatus;
import edu.mines.packtrain.repositories.ExtensionRepo;
import edu.mines.packtrain.repositories.LateRequestRepo;
import edu.mines.packtrain.seeders.CourseSeeders;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class TestExtensionService implements PostgresTestContainer {

    @Autowired
    private ExtensionService extensionService;

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
        
        mockLateRequest = lateRequestRepo.save(mockLateRequest);
        
        extensionService.processExtensionApplied(UUID.randomUUID(), true, 2);
        LateRequest capturedRequest = lateRequestRepo.getLateRequestById(mockLateRequest.getId());
       
        Assertions.assertEquals(capturedRequest.getStatus(), LateRequestStatus.APPLIED);
        Assertions.assertEquals(capturedRequest.getDaysRequested(), 2);

    }

}
