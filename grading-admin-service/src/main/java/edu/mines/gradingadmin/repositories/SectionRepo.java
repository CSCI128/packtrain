package edu.mines.gradingadmin.repositories;

import edu.mines.gradingadmin.models.Section;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SectionRepo extends CrudRepository<Section, UUID> {
}
