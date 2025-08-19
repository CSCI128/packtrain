import "@mantine/core/styles.css";
import { JSX } from "react";
import { getApiClient } from "@repo/api/index.ts";
import { MasterMigration } from "@repo/api/openapi";
import { store$ } from "@repo/api/store";
import { useQuery } from "@tanstack/react-query";
import { JSX, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useGetMasterMigration } from "../../hooks";

const getStep = (currentPage: string): number => {
  switch (currentPage) {
    case "/instructor/migrate/load":
      return 1;
    case "/instructor/migrate/apply":
      return 2;
    case "/instructor/migrate/review":
      return 3;
    case "/instructor/migrate/post":
      return 4;
    default:
      return 0;
  }
};

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
  } = useGetMasterMigration();

  // Disallow moving forward in migration flow if state is not ready
  useEffect(() => {
    if (migrationError) {
      navigate("/instructor/migrate");
      return;
    }

    if (!migrationIsLoading && migrationData) {
      const status = migrationData.status;

      // see if user tries to access the page on the next step or greater and has invalid status
      if (getStep(currentPage) >= 2 && status === "created") {
        navigate("/instructor/migrate/load");
      } else if (getStep(currentPage) >= 3 && status === "loaded") {
        navigate("/instructor/migrate/apply");
      } else if (getStep(currentPage) === 4 && status === "started") {
        navigate("/instructor/migrate/review");
      }
    }
  }, [
    migrationError,
    migrationData,
    migrationIsLoading,
    currentPage,
    navigate,
  ]);

  return children;
};
