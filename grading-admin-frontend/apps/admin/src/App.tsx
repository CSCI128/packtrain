import { MantineProvider } from "@mantine/core";
import "@mantine/core/styles.css";
import { store$, userManager } from "@repo/api/api";
import { DisabledPage } from "@repo/ui/pages/DisabledPage";
import { NotFoundPage } from "@repo/ui/pages/NotFoundPage";
import { SelectClass } from "@repo/ui/pages/Select";
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
import { MembersPage } from "../../instructor/src/Members";
import "./index.css";
<<<<<<< HEAD
import { AssignmentsPage } from "./pages/admin/Assignments";
import { CoursePage } from "./pages/admin/course/Course";
import { CreatePage } from "./pages/admin/course/Create";
import { EditCourse } from "./pages/admin/course/Edit";
import { SelectClass } from "./pages/admin/course/Select";
import { CreatePolicy } from "./pages/admin/policies/Create.tsx";
import { UsersPage } from "./pages/admin/Users";
import { HomePage } from "./pages/Home";
import { ApprovalPage } from "./pages/instructor/ApprovalPage";
import { MembersPage } from "./pages/instructor/Members";
import { MigrationsApplyPage } from "./pages/instructor/migration/MigrationsApply";
import { MigrationsLoadPage } from "./pages/instructor/migration/MigrationsLoad";
import { MigrationsPostPage } from "./pages/instructor/migration/MigrationsPost";
import { MigrationsReviewPage } from "./pages/instructor/migration/MigrationsReview";
import { MigrationsPage } from "./pages/instructor/Migrations";
=======
import { AssignmentsPage } from "./pages/Assignments";
import { CoursePage } from "./pages/course/Course";
import { CreatePage } from "./pages/course/Create";
import { EditCourse } from "./pages/course/Edit";
>>>>>>> 4e3d923 (Separating projects)
import { ProfilePage } from "./pages/Profile";
import { UsersPage } from "./pages/Users";
import ProtectedRoute from "./ProtectedRoute";
import Root from "./templates/Root";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
    },
  },
});

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

  // const { isError } = $api.useQuery("get", "/user");
  // if (isError && auth.isAuthenticated) {
  //   navigate("/disabled");
  // }

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
            path: "/approval",
            // TODO will move this to instructor application
            // path: "/instructor/approval",
            element: <ApprovalPage />,
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
