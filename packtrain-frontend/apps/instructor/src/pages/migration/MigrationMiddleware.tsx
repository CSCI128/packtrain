import "@mantine/core/styles.css";
import { getApiClient } from "@repo/api/index.ts";
import { MasterMigration } from "@repo/api/openapi";
import { store$ } from "@repo/api/store";
import { useQuery } from "@tanstack/react-query";
import { JSX } from "react";
import { useNavigate } from "react-router-dom";

export const MigrationMiddleware = ({
  children,
}: {
  children: JSX.Element;
}) => {
  const navigate = useNavigate();

  const {
    data: migrationData,
    error: migrationError,
    isLoading: migrationIsLoading,
  } = useQuery<MasterMigration | null>({
    queryKey: ["getMasterMigrations"],
    queryFn: () =>
      getApiClient()
        .then((client) =>
          client.get_master_migrations({
            course_id: store$.id.get() as string,
            master_migration_id: store$.master_migration_id.get() as string,
          })
        )
        .then((res) => res.data)
        .catch((err) => {
          console.log(err);
          return null;
        }),
  });

  if (migrationError) {
    navigate("/instructor/migrate");
  }

  if (!migrationIsLoading) {
    console.log("MIDDLEWARE DATA:", migrationData);
  }

  return children;
};
