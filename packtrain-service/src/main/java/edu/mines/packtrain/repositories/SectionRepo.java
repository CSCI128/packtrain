package edu.mines.packtrain.repositories;

import edu.mines.packtrain.models.Section;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface SectionRepo extends CrudRepository<Section, UUID> {
    boolean existsByCanvasId(long canvasId);

    @Query("select s from section s where s.course.id = ?1")
    List<Section> getSectionsForCourse(UUID courseId);

    // this is so cursed
    @Query(value = "select s.id as id, s.name as name, s.canvas_id as canvas_id, s.course_id as course_id from member_section as ms join sections as s on s.id = ms.section_id where ms.member_id = ?1", nativeQuery = true)
    Set<Section> getSectionsByMember(UUID memberId);

    Optional<Section> getById(UUID id);
}
