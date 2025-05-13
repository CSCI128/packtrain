package edu.mines.packtrain.repositories;

import edu.mines.packtrain.models.GradescopeConfig;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface GradescopeConfigRepo extends CrudRepository<GradescopeConfig, UUID> {
}
