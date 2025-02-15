package edu.mines.gradingadmin.repositories;

import edu.mines.gradingadmin.models.Course;
import edu.mines.gradingadmin.models.CourseMember;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.UUID;

@Repository
public interface CourseMemberRepo extends CrudRepository<CourseMember, UUID> {
    Set<CourseMember> getAllByCourse(Course course);

    @Query("select m from course_member m where m.course = ?1 and m.user.name like concat('%',?2,'%')")
    Set<CourseMember> findAllByCourseByUserName(Course course, String name);

    @Query("select m from course_member m where m.course = ?1 and m.user.cwid like concat('%',?2,'%')")
    Set<CourseMember> findAllByCourseByCwid(Course course, String cwid);
}
