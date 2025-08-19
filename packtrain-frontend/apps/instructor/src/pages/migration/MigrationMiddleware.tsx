import "@mantine/core/styles.css";
import { JSX, useEffect, useState } from "react";
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

  // stored for debouncing - middleware queries will run too quickly
  // and move the user back upon clicking "next" if this doesn't exist
  const [lastState, setLastState] = useState<string>();

  // Disallow moving forward in migration flow if state is not ready
  useEffect(() => {
    if (migrationError) {
      navigate("/instructor/migrate");
      return;
    }

    if (
      !migrationIsLoading &&
      migrationData &&
      lastState !== migrationData.status
    ) {
      const status = migrationData.status;

      // see if user tries to access the page on the next step or greater and has invalid status
      if (getStep(currentPage) >= 2 && status === "created") {
        navigate("/instructor/migrate/load");
      } else if (getStep(currentPage) >= 3 && status === "loaded") {
        navigate("/instructor/migrate/apply");
      } else if (getStep(currentPage) === 4 && status === "started") {
        navigate("/instructor/migrate/review");
      }
      setLastState(status);
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
