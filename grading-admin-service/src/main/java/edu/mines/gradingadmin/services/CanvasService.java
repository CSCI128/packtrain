package edu.mines.gradingadmin.services;


import edu.mines.gradingadmin.managers.SecurityManager;
import edu.mines.gradingadmin.models.ExternalSource;
import edu.mines.gradingadmin.models.ExternalSourceType;
import edu.mines.gradingadmin.repositories.ExternalSourceRepo;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CanvasService {
    private final boolean enabled;
    private final SecurityManager manager;
    private final String baseEndpoint;

    public CanvasService(SecurityManager manager, ExternalSourceRepo externalSourceRepo) {
        this.manager = manager;

        Optional<ExternalSource> source = externalSourceRepo.getByType(ExternalSourceType.CANVAS);

        enabled = source.isPresent();

        baseEndpoint = enabled ? source.get().getEndpoint() : "disabled";
    }



}