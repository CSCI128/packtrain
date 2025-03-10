package edu.mines.gradingadmin.repositories;

import edu.mines.gradingadmin.models.RawScore;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RawScoreRepo extends CrudRepository<RawScore, UUID> {
    List<RawScore> getByCwid(String cwid);
    List<RawScore> getByMigrationId(UUID migrationId);

    @Query("select r from raw_score r where r.cwid = ?1 and r.migrationId = ?2")
    Optional<RawScore> getByCwidAndMigrationId(String cwid, UUID migrationId);

    // TODO: Write an existsByCwidAndMigrationId query
}
