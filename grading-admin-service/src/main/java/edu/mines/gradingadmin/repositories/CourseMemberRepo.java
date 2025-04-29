package edu.mines.gradingadmin.repositories;

import edu.mines.gradingadmin.models.Course;
import edu.mines.gradingadmin.models.CourseMember;
import edu.mines.gradingadmin.models.User;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface CourseMemberRepo extends CrudRepository<CourseMember, UUID> {
    boolean existsByCourseAndUser(Course course, User user);

    Optional<CourseMember> getAllByCourseAndUser(Course course, User user);

    @Query("select m.user.cwid from course_member m where m.course = ?1")
    Set<String> getAllCwidsByCourse(Course course);

    Set<CourseMember> getAllByCourse(Course course);

    @Query("select m from course_member m where m.course = ?1 and m.user.name like concat('%',?2,'%')")
    Set<CourseMember> findAllByCourseByUserName(Course course, String name);

    @Query("select m from course_member m where m.course = ?1 and m.user.cwid like concat('%',?2,'%')")
    Set<CourseMember> findAllByCourseByCwid(Course course, String cwid);

    @Query("select m from course_member m where m.course = ?1 and m.user.cwid in ?2")
    Set<CourseMember> getAllByCourseAndCwids(Course course, Set<String> cwids);

    @Query("select m from course_member m where m.user.cwid = ?1")
    Set<CourseMember> getAllByCwid(String cwid);

    Optional<CourseMember> getByUserAndCourse(User user, Course course);

    @Query("delete from course_member m where m.course = ?1 and m.user.cwid in ?2")
    @Modifying
    void deleteByCourseAndCwid(Course course, Set<String> cwids);

    @Query("select cm.course from course_member cm where cm.user.cwid = ?1")
    List<Course> getEnabledCoursesByUserId(String id);
}
