package edu.mines.gradingadmin.repositories;

import edu.mines.gradingadmin.models.MasterMigrationStats;

import java.util.UUID;

public interface MasterMigrationStatsRepo extends ViewOnlyRepository<MasterMigrationStats, UUID> {
}
