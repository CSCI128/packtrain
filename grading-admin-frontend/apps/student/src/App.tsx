import { MantineProvider } from "@mantine/core";
import "@mantine/core/styles.css";
import { store$ } from "@repo/api/api";
import { NotFoundPage } from "@repo/ui/pages/NotFoundPage";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useEffect } from "react";
import { useAuth } from "react-oidc-context";
import {
  createBrowserRouter,
  RouterProvider,
  useLocation,
  useNavigate,
} from "react-router-dom";
import "./App.css";
import "./index.css";
import { SelectClass } from "./pages/admin/course/Select";
import { HomePage } from "./pages/Home";
import { ProfilePage } from "./pages/Profile";
import { ExtensionForm } from "./pages/student/ExtensionForm";
import { Requests } from "./pages/student/Requests";
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
  const location = useLocation();
  const isAuthenticated = store$.id.get();
  const auth = useAuth();
  const currentPage = location.pathname;

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
