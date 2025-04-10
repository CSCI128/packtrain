import { MantineProvider } from "@mantine/core";
import "@mantine/core/styles.css";
import { store$, userManager } from "@repo/api/api";
import { DisabledPage } from "@repo/ui/pages/DisabledPage";
import { NotFoundPage } from "@repo/ui/pages/NotFoundPage";
import { ProfilePage } from "@repo/ui/pages/Profile";
import { SelectClass } from "@repo/ui/pages/Select";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import axios from "axios";
import { JSX, useEffect } from "react";
import { useAuth } from "react-oidc-context";
import {
  createBrowserRouter,
  Outlet,
  RouterProvider,
  useLocation,
  useNavigate,
} from "react-router-dom";
import { MigrationsApplyPage } from "../../instructor/src/migration/MigrationsApply";
import { MigrationsLoadPage } from "../../instructor/src/migration/MigrationsLoad";
import { MigrationsPostPage } from "../../instructor/src/migration/MigrationsPost";
import { MigrationsReviewPage } from "../../instructor/src/migration/MigrationsReview";
import { MigrationsPage } from "../../instructor/src/Migrations";
import "./index.css";
import Root from "./templates/Root";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
    },
  },
});

const MigrationMiddleware = ({ children }: { children: JSX.Element }) => {
  // Four step migrate process: send people to first or active state or
  // prevent them from going to future states
  return children;
};

const fetchData = async () => {
  const response = await axios.get("/api/user");
  return response.data;
};

const MiddlewareLayout = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const isAuthenticated = store$.id.get();
  const auth = useAuth();
  const currentPage = location.pathname;

  useEffect(() => {
    const fetchData = async () => {
      try {
        // const { isError } = $api.useQuery("get", "/user");
        // if (isError && auth.isAuthenticated) {
        //   navigate("/disabled");
        // }

        const user = await userManager.getUser();
        if (!isAuthenticated && user && user.profile.is_admin) {
          navigate("/select");
        }
      } catch (error) {
        console.error("An error occurred:", error);
      }
    };

    if (currentPage !== "/profile") {
      fetchData();
    }
  }, [isAuthenticated, navigate]);

  return <Outlet />;
};

const CallbackPage = () => {
  const auth = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (auth.isAuthenticated) {
      if (auth.user?.profile.is_admin && !store$.id.get()) {
        navigate("/select");
      } else if (!auth.user?.profile.is_admin && !store$.id.get()) {
        navigate("/");
      }
    }
  }, [auth.isAuthenticated]);

  return null;
};

const router = createBrowserRouter([
  {
    element: <Root />,
    children: [
      {
        element: <MiddlewareLayout />,
        children: [
          {
            path: "/select",
            element: <SelectClass />,
          },
          {
            path: "/callback",
            element: <CallbackPage />,
          },
          {
            path: "/instructor/migrate",
            element: <MigrationsPage />,
          },
          {
            path: "/instructor/migrate/load",
            element: (
              <MigrationMiddleware>
                <MigrationsLoadPage />
              </MigrationMiddleware>
            ),
          },
          {
            path: "/instructor/migrate/apply",
            element: (
              <MigrationMiddleware>
                <MigrationsApplyPage />
              </MigrationMiddleware>
            ),
          },
          {
            path: "/instructor/migrate/review",
            element: (
              <MigrationMiddleware>
                <MigrationsReviewPage />
              </MigrationMiddleware>
            ),
          },
          {
            path: "/instructor/migrate/post",
            element: (
              <MigrationMiddleware>
                <MigrationsPostPage />
              </MigrationMiddleware>
            ),
          },
          {
            path: "/profile",
            element: <ProfilePage />,
          },
          {
            path: "/disabled",
            element: <DisabledPage />,
          },
          {
            path: "*",
            element: <NotFoundPage />,
          },
        ],
      },
    ],
  },
]);

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <MantineProvider>
        <RouterProvider router={router} />
      </MantineProvider>
    </QueryClientProvider>
  );
}
