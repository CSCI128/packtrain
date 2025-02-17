import { MantineProvider } from "@mantine/core";
import "@mantine/core/styles.css";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { createBrowserRouter, RouterProvider } from "react-router-dom";
import "./index.css";
import { AssignmentsPage } from "./pages/admin/assignments";
import { CoursePage } from "./pages/admin/course/Course";
import { CreatePage } from "./pages/admin/course/Create";
import { EditCourse } from "./pages/admin/course/Edit";
import { ImportPage } from "./pages/admin/course/Import";
import { MigratePage } from "./pages/admin/Migrate";
import { Profile } from "./pages/admin/Profile";
import { UsersPage } from "./pages/admin/Users";
import { HomePage } from "./pages/Home";
import Root from "./templates/Root";

const queryClient = new QueryClient();

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
        element: <CoursePage />,
      },
      {
        path: "/admin/edit",
        element: <EditCourse />,
      },
      {
        path: "/instructor/migrate",
        element: <MigratePage />,
      },
      {
        path: "/admin/create",
        element: <CreatePage />,
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
        path: "/admin/users",
        element: <UsersPage />,
      },
      {
        path: "/profile",
        element: <Profile />,
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
