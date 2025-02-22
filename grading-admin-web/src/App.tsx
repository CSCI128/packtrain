import { MantineProvider } from "@mantine/core";
import "@mantine/core/styles.css";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useEffect } from "react";
import {
  createBrowserRouter,
  Outlet,
  RouterProvider,
  useLocation,
  useNavigate,
} from "react-router-dom";
import { store$, userManager } from "./api";
import "./index.css";
import { CoursePage } from "./pages/admin/course/Course";
import { CreatePage } from "./pages/admin/course/Create";
import { EditCourse } from "./pages/admin/course/Edit";
import { ImportPage } from "./pages/admin/course/Import";
import { SelectClass } from "./pages/admin/course/Select";
import { UsersPage } from "./pages/admin/Users";
import { HomePage } from "./pages/Home";
import { AssignmentsPage } from "./pages/instructor/Assignments";
import { MembersPage } from "./pages/instructor/Members";
import { MigrationsPage } from "./pages/instructor/Migrations";
import { ProfilePage } from "./pages/Profile";
import ProtectedRoute from "./ProtectedRoute";
import Root from "./templates/Root";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
    },
  },
});

// TODO not sure if middleware is the correct name here but yeehaw
const MiddlewareLayout = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const isAuthenticated = store$.id.get();
  const currentPage = location.pathname;

  // TODO can't find course, delete state tracked course; will this ever happen?
  // useEffect(() => {
  //   if (data === undefined) {
  //     store$.id.delete();
  //     store$.name.delete();
  //   }
  // }, []);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const user = await userManager.getUser();
        if (!isAuthenticated && user) {
          navigate("select");
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
