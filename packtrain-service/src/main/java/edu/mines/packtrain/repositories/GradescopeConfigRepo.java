package edu.mines.packtrain.repositories;

import edu.mines.packtrain.models.GradescopeConfig;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

public interface GradescopeConfigRepo extends CrudRepository<GradescopeConfig, UUID> {
}
