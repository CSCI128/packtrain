import { MantineProvider } from "@mantine/core";
import "@mantine/core/styles.css";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { createBrowserRouter, RouterProvider } from "react-router-dom";
import "./index.css";
import { AssignmentsPage } from "./pages/admin/Assignments";
import { CoursePage } from "./pages/admin/course/Course";
import { CreatePage } from "./pages/admin/course/Create";
import { EditCourse } from "./pages/admin/course/Edit";
import { ImportPage } from "./pages/admin/course/Import";
import { MembersPage } from "./pages/admin/Members";
import { MigrationsPage } from "./pages/admin/Migrations";
import { ProfilePage } from "./pages/admin/Profile";
import { UsersPage } from "./pages/admin/Users";
import { HomePage } from "./pages/Home";
import ProtectedRoute from "./ProtectedRoute";
import Root from "./templates/Root";

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
        path: "/",
        element: <HomePage />,
      },
      {
        path: "/admin/home",
        element: (
          <ProtectedRoute>
            <CoursePage />
          </ProtectedRoute>
        ),
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
        path: "/instructor/migrate",
        element: <MigrationsPage />,
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
        path: "/admin/import",
        element: <ImportPage />,
      },
      {
        path: "/admin/assignments",
        element: <AssignmentsPage />,
      },
      {
        path: "/admin/members",
        element: <MembersPage />,
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
        path: "/profile",
        element: <ProfilePage />,
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
