import { MantineProvider } from "@mantine/core";
import "@mantine/core/styles.css";
import { configureApiClient } from "@repo/api/index";
import { MiddlewareLayout } from "@repo/ui/MiddlewareLayout";
import { DisabledPage } from "@repo/ui/pages/DisabledPage";
import { NotFoundPage } from "@repo/ui/pages/NotFoundPage";
import { SelectClass } from "@repo/ui/pages/Select";
import { ProfilePage } from "@repo/ui/Profile";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import ReactDOM from "react-dom/client";
import { AuthProvider } from "react-oidc-context";
import { createBrowserRouter, RouterProvider } from "react-router-dom";
import "./app.scss";
import { AUTH_CONFIG, userManager } from "./auth.ts";
import { AssignmentsPage } from "./pages/Assignments.tsx";
import { CoursePage } from "./pages/course/Course.tsx";
import { CreatePage } from "./pages/course/Create.tsx";
import { CreatePolicy } from "./pages/course/CreatePolicy.tsx";
import { EditCourse } from "./pages/course/Edit.tsx";
import { MembersPage } from "./pages/Members.tsx";
import TestPage from "./pages/Test.tsx";
import { UsersPage } from "./pages/Users.tsx";
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
        element: <MiddlewareLayout />,
        children: [
          {
            path: "/admin/test",
            element: <TestPage />,
          },
          {
            path: "/admin",
            element: <CoursePage />,
          },
          {
            path: "/select",
            element: <SelectClass />,
          },
          {
            path: "/admin/edit",
            element: <EditCourse />,
          },
          {
            path: "/admin/policies/new",
            element: <CreatePolicy />,
          },
          {
            path: "/admin/create",
            element: <CreatePage />,
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
            element: <UsersPage />,
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

ReactDOM.createRoot(document.getElementById("root")!).render(
  <AuthProvider {...AUTH_CONFIG}>
    <QueryClientProvider client={queryClient}>
      <MantineProvider>
        <RouterProvider router={router} />
      </MantineProvider>
    </QueryClientProvider>
  </AuthProvider>
);
