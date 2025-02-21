import { MantineProvider } from "@mantine/core";
import "@mantine/core/styles.css";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useEffect } from "react";
import {
  createBrowserRouter,
  Outlet,
  RouterProvider,
  useNavigate,
} from "react-router-dom";
import { store$, userManager } from "./api";
import "./index.css";
import { AssignmentsPage } from "./pages/admin/Assignments";
import { CoursePage } from "./pages/admin/course/Course";
import { CreatePage } from "./pages/admin/course/Create";
import { EditCourse } from "./pages/admin/course/Edit";
import { ImportPage } from "./pages/admin/course/Import";
import { SelectClass } from "./pages/admin/course/Select";
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

const MiddlewareLayout = () => {
  const navigate = useNavigate();
  const isAuthenticated = store$.id.get();

  useEffect(() => {
    const fetchData = async () => {
      try {
        const user = await userManager.getUser();
        console.log(user);
        // const user = await userManager.getUser();
        if (!isAuthenticated && user) {
          // && user
          navigate("select");
        }
      } catch (error) {
        console.error("An error occurred:", error);
      }
    };

    fetchData();
  }, [isAuthenticated, navigate]);

  return <Outlet />;
};

const router = createBrowserRouter([
  {
    element: <Root />,
    children: [
      {
        element: <MiddlewareLayout />,
        children: [
          {
            path: "/",
            element: <HomePage />,
          },
          {
            path: "/select",
            element: <SelectClass />,
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
            element: (
              <ProtectedRoute>
                <ImportPage />
              </ProtectedRoute>
            ),
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
      //   children: [
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
