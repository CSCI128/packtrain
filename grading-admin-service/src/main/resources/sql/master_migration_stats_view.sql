DO
$$
    BEGIN IF EXISTS(SELECT 1 FROM pg_tables WHERE tablename = 'master_migration_stats_view') THEN
        EXECUTE 'DROP TABLE IF EXISTS master_migration_stats_view';
    END IF;
END $$;

CREATE OR REPLACE VIEW master_migration_stats_view AS
SELECT r.master_migration_id                                                    AS master_migration_id,
       COUNT(r.cwid)                                                            AS total_submissions,
       SUM(CASE WHEN late_request.request_type IS NOT NULL THEN 1 ELSE 0 END)   AS late_requests,
       SUM(CASE WHEN late_request.request_type = 'EXTENSION' THEN 1 ELSE 0 END) AS total_extensions,
       SUM(CASE WHEN late_request.request_type = 'LATE_PASS' THEN 1 ELSE 0 END) AS total_late_passes,
       SUM(CASE WHEN late_request.status <> 'APPROVED' THEN 1 ELSE 0 END)       AS unapproved_requests
FROM (SELECT raw_score.cwid              AS cwid,
             raw_score.submission_status AS submission_status,
             migration.assignment        AS assignment_id,
             migration.master_migration  AS master_migration_id
      FROM raw_scores as raw_score
               JOIN migrations AS migration ON raw_score.migration_id = migration.id) AS r
         RIGHT JOIN late_requests AS late_request ON late_request.assignment = r.assignment_id
         RIGHT JOIN extensions AS extension ON late_request.extension = extension.id
GROUP BY r.master_migration_id;
