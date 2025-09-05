package edu.mines.packtrain.repositories;

import edu.mines.packtrain.models.RawScore;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RawScoreRepo extends CrudRepository<RawScore, UUID> {
    List<RawScore> getByCwid(String cwid);
    List<RawScore> getByMigrationId(UUID migrationId);

    @Query("select r from raw_score r where r.cwid = ?1 and r.migrationId = ?2")
    Optional<RawScore> getByCwidAndMigrationId(String cwid, UUID migrationId);

    @Query("select count(r) > 0 from raw_score r where r.cwid = ?1 and r.migrationId = ?2")
    boolean existsByCwidAndMigrationId(String cwid, UUID migrationId);
}
