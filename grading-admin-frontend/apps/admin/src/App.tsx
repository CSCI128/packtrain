import { MantineProvider, Text } from "@mantine/core";
import "@mantine/core/styles.css";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import axios from "axios";
import { useEffect } from "react";
import { useAuth } from "react-oidc-context";
import {
  createBrowserRouter,
  Outlet,
  RouterProvider,
  useLocation,
  useNavigate,
} from "react-router-dom";
import { $api, store$, userManager } from "./api";
import "./index.css";
import { AssignmentsPage } from "./pages/admin/Assignments";
import { CoursePage } from "./pages/admin/course/Course";
import { CreatePage } from "./pages/admin/course/Create";
import { EditCourse } from "./pages/admin/course/Edit";
import { SelectClass } from "./pages/admin/course/Select";
import { UsersPage } from "./pages/admin/Users";
import { HomePage } from "./pages/Home";
import { ApprovalPage } from "./pages/instructor/ApprovalPage";
import { MembersPage } from "./pages/instructor/Members";
import { MigrationsApplyPage } from "./pages/instructor/migration/MigrationsApply";
import { MigrationsLoadPage } from "./pages/instructor/migration/MigrationsLoad";
import { MigrationsPostPage } from "./pages/instructor/migration/MigrationsPost";
import { MigrationsReviewPage } from "./pages/instructor/migration/MigrationsReview";
import { MigrationsPage } from "./pages/instructor/Migrations";
import { ProfilePage } from "./pages/Profile";
import { ExtensionForm } from "./pages/student/ExtensionForm";
import { Requests } from "./pages/student/Requests";
import ProtectedRoute from "./ProtectedRoute";
import Root from "./templates/Root";

const queryClient = new QueryClient({
  // queryCache: new QueryCache({
  //   onError: async (error, query) => {
  //     console.log(`Something went wrong: ${error.message}`);
  //     console.log(error.response?.status);
  //   },
  // }),
  defaultOptions: {
    queries: {
      retry: false,
    },
  },
});

const NotFoundPage = () => {
  return (
    <>
      <Text>The specified page could not be found!</Text>
    </>
  );
};

const UserIsDisabled = () => {
  const auth = useAuth();
  const email = auth.user?.profile.email || "None";

  return email != "None" ? (
    <>
      <Text>
        User '{email}' has been disabled! Contact an administrator for
        assistance!
      </Text>
    </>
  ) : (
    <>
      <Text>You are not logged in!</Text>
    </>
  );
};

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

  const { isError } = $api.useQuery("get", "/user");
  if (isError && auth.isAuthenticated) {
    navigate("/disabled");
  }

  // const { data, error } = useQuery({
  //   queryKey: ["data"],
  //   queryFn: fetchData,
  //   onError: (error) => {
  //     console.log(error.response.status);
  //     if (axios.isAxiosError(error)) {
  //       console.log("HTTP Status:", error.response?.status);
  //     }
  //   },
  // });

  // if (error) {
  //   return <div>Error: {error.message}</div>;
  // }

  // return <div>Data: {JSON.stringify(data)}</div>;

  // const { data, error: err } = $api.useQuery("get", "/user");

  // if (data) {
  //   console.log("DATA:", data);
  // }

  // if (err) {
  //   console.log("ERR:", err);
  // }

  useEffect(() => {
    const fetchData = async () => {
      try {
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
            path: "/",
            element: <HomePage />,
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
            path: "/requests",
            element: <Requests />,
          },
          {
            path: "/extension",
            element: <ExtensionForm />,
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
            path: "/approval",
            // TODO will move this to instructor application
            // path: "/instructor/approval",
            element: <ApprovalPage />,
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
            path: "/profile",
            element: <ProfilePage />,
          },
          {
            path: "/disabled",
            element: <UserIsDisabled />,
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
