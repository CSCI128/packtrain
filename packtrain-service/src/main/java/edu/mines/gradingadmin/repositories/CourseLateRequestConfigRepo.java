package edu.mines.gradingadmin.repositories;

import edu.mines.gradingadmin.models.CourseLateRequestConfig;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface CourseLateRequestConfigRepo extends CrudRepository<CourseLateRequestConfig, UUID> {
}
