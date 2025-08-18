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
  const currentPage = location.pathname;

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

  // Disallow moving forward in migration flow if state is not ready
  if (!migrationIsLoading && migrationData) {
    const status = migrationData.status;

    if (currentPage !== "/instructor/migrate/load" && status === "created") {
      navigate("/instructor/migrate/load");
    } else if (
      currentPage !== "/instructor/migrate/apply" &&
      status === "loaded"
    ) {
      navigate("/instructor/migrate/apply");
    } else if (
      currentPage !== "/instructor/migrate/review" &&
      status === "started"
    ) {
      navigate("/instructor/migrate/review");
    } else if (
      currentPage !== "/instructor/migrate/post" &&
      status !== "created" &&
      status !== "loaded" &&
      status !== "started"
    ) {
      navigate("/instructor/migrate/post");
    }
  }

  return children;
};
