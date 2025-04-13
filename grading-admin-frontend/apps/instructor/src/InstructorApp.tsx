import { MantineProvider } from "@mantine/core";
import "@mantine/core/styles.css";
import { configureApiClient } from "@repo/api/index.ts";
import { MiddlewareLayout } from "@repo/ui/MiddlewareLayout";
import { CallbackPage } from "@repo/ui/pages/CallbackPage";
import { DisabledPage } from "@repo/ui/pages/DisabledPage";
import { NotFoundPage } from "@repo/ui/pages/NotFoundPage";
import { SelectClass } from "@repo/ui/pages/Select";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { JSX } from "react";
import { createBrowserRouter, RouterProvider } from "react-router-dom";
import { userManager } from "./api";
import "./index.css";
import { ApprovalPage } from "./pages/ApprovalPage";
import { MigrationsPage } from "./pages/migration/Migrations";
import { MigrationsApplyPage } from "./pages/migration/MigrationsApply";
import { MigrationsLoadPage } from "./pages/migration/MigrationsLoad";
import { MigrationsPostPage } from "./pages/migration/MigrationsPost";
import { MigrationsReviewPage } from "./pages/migration/MigrationsReview";
import { ProfilePage } from "./pages/Profile";
import Root from "./templates/Root";

configureApiClient({ userManager: userManager });

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
    },
  },
});

const MigrationMiddleware = ({ children }: { children: JSX.Element }) => {
  // Four step migrate process: send people to first or last or active state
  // and prevent them from going to future states
  return children;
};

const router = createBrowserRouter([
  {
    element: <Root />,
    children: [
      {
        element: <MiddlewareLayout userManager={userManager} />,
        children: [
          {
            path: "/instructor",
            element: <ApprovalPage />,
          },
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
