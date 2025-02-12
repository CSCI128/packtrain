package edu.mines.gradingadmin.repositories;

import edu.mines.gradingadmin.models.Section;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SectionRepo extends CrudRepository<Section, UUID> {
    boolean existsByCanvasId(long canvasId);

    @Query("select s from section s where s.course.id = ?1")
    List<Section> getSectionsForCourse(UUID courseId);

    Optional<Section> getById(UUID id);
}
