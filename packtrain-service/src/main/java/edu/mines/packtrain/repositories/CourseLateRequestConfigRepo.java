package edu.mines.packtrain.repositories;

import edu.mines.packtrain.models.CourseLateRequestConfig;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface CourseLateRequestConfigRepo extends CrudRepository<CourseLateRequestConfig, UUID> {
}
