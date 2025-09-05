package edu.mines.packtrain.repositories;

import edu.mines.packtrain.models.CourseLateRequestConfig;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

public interface CourseLateRequestConfigRepo extends CrudRepository<CourseLateRequestConfig, UUID> {
}
