import { MantineProvider } from "@mantine/core";
import "@mantine/core/styles.css";
import { configureApiClient } from "@repo/api/index.ts";
import { MiddlewareLayout } from "@repo/ui/MiddlewareLayout";
import { DisabledPage } from "@repo/ui/pages/DisabledPage";
import { NotFoundPage } from "@repo/ui/pages/NotFoundPage";
import { SelectClass } from "@repo/ui/pages/Select";
import { ProfilePage } from "@repo/ui/Profile";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { JSX } from "react";
import ReactDOM from "react-dom/client";
import { AuthProvider } from "react-oidc-context";
import { createBrowserRouter, RouterProvider } from "react-router-dom";
import "./app.scss";
import { userManager } from "./auth";
import { AUTH_CONFIG } from "./auth.ts";
import { ApprovalPage } from "./pages/Approvals";
import { MembersPage } from "./pages/Members";
import { MigrationsPage } from "./pages/migration/Migrations";
import { MigrationsApplyPage } from "./pages/migration/MigrationsApply";
import { MigrationsLoadPage } from "./pages/migration/MigrationsLoad";
import { MigrationsPostPage } from "./pages/migration/MigrationsPost";
import { MigrationsReviewPage } from "./pages/migration/MigrationsReview";
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
        element: <MiddlewareLayout />,
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
            path: "/instructor/members",
            element: <MembersPage />,
          },
          {
            path: "/instructor/profile",
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

ReactDOM.createRoot(document.getElementById("root")!).render(
  <AuthProvider {...AUTH_CONFIG}>
    <QueryClientProvider client={queryClient}>
      <MantineProvider>
        <RouterProvider router={router} />
      </MantineProvider>
    </QueryClientProvider>
  </AuthProvider>
);
