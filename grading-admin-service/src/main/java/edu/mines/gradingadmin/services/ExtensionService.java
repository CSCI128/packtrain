package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.models.Extension;
import edu.mines.gradingadmin.repositories.ExtensionRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
import java.time.Instant;

@Service
@Slf4j
public class ExtensionService {
    private final ExtensionRepo extensionRepo;

    public ExtensionService(ExtensionRepo extensionRepo){
        this.extensionRepo = extensionRepo;
    }

    public List<Extension> getAllExtensions(String migrationId){
        List<Extension> extensions = extensionRepo.getExtensionByMigrationId(UUID.fromString(migrationId));
        return extensions.stream().toList();
    }

}
