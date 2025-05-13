package edu.mines.gradingadmin.repositories;

import edu.mines.gradingadmin.models.CourseLateRequestConfig;
import edu.mines.gradingadmin.models.GradescopeConfig;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface GradescopeConfigRepo extends CrudRepository<GradescopeConfig, UUID> {
}
