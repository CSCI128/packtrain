import "@mantine/core/styles.css";
import { JSX } from "react";
import { useNavigate } from "react-router-dom";
import { useGetMasterMigration } from "../../hooks";

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
    }
  }

  return children;
};
