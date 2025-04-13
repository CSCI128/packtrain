import { MantineProvider } from "@mantine/core";
import "@mantine/core/styles.css";
import { configureApiClient } from "@repo/api/index";
import { MiddlewareLayout } from "@repo/ui/MiddlewareLayout";
import { CallbackPage } from "@repo/ui/pages/CallbackPage";
import { DisabledPage } from "@repo/ui/pages/DisabledPage";
import { NotFoundPage } from "@repo/ui/pages/NotFoundPage";
import { SelectClass } from "@repo/ui/pages/Select";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { createBrowserRouter, RouterProvider } from "react-router-dom";
import { MembersPage } from "../../instructor/src/pages/Members.tsx";
import { userManager } from "./api.ts";
import "./index.css";
import { AssignmentsPage } from "./pages/Assignments";
import { CoursePage } from "./pages/course/Course";
import { CreatePage } from "./pages/course/Create";
import { EditCourse } from "./pages/course/Edit";
import { CreatePolicy } from "./pages/policies/Create.tsx";
import { ProfilePage } from "./pages/Profile";
import { UsersPage } from "./pages/Users";
import ProtectedRoute from "./ProtectedRoute";
import Root from "./templates/Root.tsx";

configureApiClient({ userManager: userManager });

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
    },
  },
});

const router = createBrowserRouter([
  {
    element: <Root />,
    children: [
      {
        element: <MiddlewareLayout userManager={userManager} />,
        children: [
          {
            path: "/admin",
            element: <CoursePage />,
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
            path: "/admin/edit",
            element: (
              <ProtectedRoute>
                <EditCourse />
              </ProtectedRoute>
            ),
          },
          {
            path: "/admin/policies/new",
            element: (
              <ProtectedRoute>
                <CreatePolicy />
              </ProtectedRoute>
            ),
          },
          {
            path: "/admin/create",
            element: (
              <ProtectedRoute>
                <CreatePage />
              </ProtectedRoute>
            ),
          },
          {
            path: "/admin/assignments",
            element: (
              <ProtectedRoute>
                <AssignmentsPage />
              </ProtectedRoute>
            ),
          },
          {
            path: "/admin/members",
            element: (
              <ProtectedRoute>
                <MembersPage />
              </ProtectedRoute>
            ),
          },
          {
            path: "/admin/users",
            element: (
              <ProtectedRoute>
                <UsersPage />
              </ProtectedRoute>
            ),
          },
          {
            path: "/admin/profile",
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
