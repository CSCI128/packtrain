package edu.mines.packtrain.repositories;

import edu.mines.packtrain.models.MasterMigrationStats;

import java.util.UUID;

public interface MasterMigrationStatsRepo extends ViewOnlyRepository<MasterMigrationStats, UUID> {
}
