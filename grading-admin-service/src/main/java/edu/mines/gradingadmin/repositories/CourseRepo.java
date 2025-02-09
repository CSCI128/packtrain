package edu.mines.gradingadmin.repositories;

import edu.mines.gradingadmin.models.Course;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseRepo extends CrudRepository<Course, UUID> {
    boolean existsByCanvasId(long id);

    @Query("select c from course c")
    List<Course> getAll();

    @Query("select c from course c where c.enabled=?1")
    List<Course> getAll(boolean enabled);

    @Query("select c from course c where c.id = ?1")
    Optional<Course> getById(UUID id);

    @Query("select c.name from course c where c.name like concat('%',?1,'%')")
    List<Course> searchByName(String name);

    @Query("select c.term from course c where c.term like ?1")
    List<Course> searchByTerm(String term);

    @Query("select c.code from course c where c.code like ?1")
    List<Course> searchByCode(String code);
}
