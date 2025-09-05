package edu.mines.packtrain.repositories;

import java.util.UUID;
import edu.mines.packtrain.models.MasterMigrationStats;

public interface MasterMigrationStatsRepo extends ViewOnlyRepository<MasterMigrationStats, UUID> {
}
